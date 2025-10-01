package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.*;
import org.example.piratelegacy.auth.entity.enums.QuestChainType;
import org.example.piratelegacy.auth.repository.ItemRepository;
import org.example.piratelegacy.auth.repository.QuestChainRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestInitializationService implements CommandLineRunner {

    private final QuestChainRepository questChainRepository;
    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${game.config.quests-path}")
    private String questsJsonFile;

    @Value("${game.quests.force-reload:false}")
    private boolean forceReload;

    @Override
    public void run(String... args) {
        if (forceReload) {
            log.info("Force reloading quests from JSON... All existing quests and chains will be deleted.");
            questChainRepository.deleteAllInBatch();
        }

        if (questChainRepository.count() == 0) {
            log.info("No quest chains found in DB. Initializing quests from JSON file: {}", questsJsonFile);
            initializeQuestsFromJson();
        } else {
            log.info("Quest chains already exist in database, skipping initialization.");
        }
    }

    @Transactional
    public void initializeQuestsFromJson() {
        try {
            Resource resource = resourceLoader.getResource(questsJsonFile);
            if (!resource.exists()) {
                log.warn("Quest JSON file not found: {}, skipping.", questsJsonFile);
                return;
            }

            Map<String, Item> existingItems = itemRepository.findAll().stream()
                    .collect(Collectors.toMap(Item::getItemKey, Function.identity()));

            InputStream inputStream = resource.getInputStream();
            List<QuestChainJsonDto> chainDtos = objectMapper.readValue(inputStream, new TypeReference<>() {});

            for (QuestChainJsonDto chainDto : chainDtos) {
                QuestChain questChain = QuestChain.builder()
                        .chainKey(chainDto.getChainKey())
                        .title(chainDto.getTitle())
                        .description(chainDto.getDescription())
                        .iconUrl(chainDto.getIconUrl())
                        .chainType(chainDto.getType())
                        .quests(new ArrayList<>())
                        .build();

                for (QuestStepJsonDto stepDto : chainDto.getSteps()) {
                    Quest quest = Quest.builder()
                            .questKey(stepDto.getQuestKey())
                            .questOrder(stepDto.getQuestOrder())
                            .title(stepDto.getTitle())
                            .npcName(stepDto.getNpcName())
                            .npcImageUrl(stepDto.getNpcImageUrl())
                            .storyText(stepDto.getStoryText())
                            .objective(stepDto.getObjective())
                            .goldReward(stepDto.getRewards().getGold())
                            .expReward(stepDto.getRewards().getExperience())
                            .woodReward(stepDto.getRewards().getWood())
                            .stoneReward(stepDto.getRewards().getStone())
                            .buttonText(stepDto.getButtonText())
                            .battleLocationId(stepDto.getBattleLocationId())
                            .questChain(questChain)
                            .itemRewards(new ArrayList<>())
                            .build();

                    if (stepDto.getRewards().getItems() != null) {
                        for (ItemRewardJsonDto itemDto : stepDto.getRewards().getItems()) {
                            Item item = existingItems.computeIfAbsent(itemDto.getItemKey(), key -> {
                                log.info("Creating new item from JSON: key='{}', name='{}'", key, itemDto.getName());
                                return itemRepository.save(Item.builder()
                                        .itemKey(key)
                                        .name(itemDto.getName())
                                        .imageUrl(itemDto.getImageUrl())
                                        .build());
                            });

                            QuestItemReward reward = QuestItemReward.builder()
                                    .quest(quest)
                                    .item(item)
                                    .quantity(itemDto.getQuantity())
                                    .build();
                            quest.getItemRewards().add(reward);
                        }
                    }
                    questChain.getQuests().add(quest);
                }
                questChainRepository.save(questChain);
                log.info("Successfully saved quest chain: '{}'", questChain.getTitle());
            }

        } catch (Exception e) {
            log.error("Failed to load quests from JSON file: {}", questsJsonFile, e);
        }
    }

    // --- ВОТ ЭТОТ МЕТОД НУЖНО БЫЛО ДОБАВИТЬ ---
    @Transactional
    public void reloadQuestsFromJson() {
        log.info("Manual quest reload requested via admin endpoint.");
        questChainRepository.deleteAllInBatch();
        initializeQuestsFromJson();
    }
    // ------------------------------------------

    // Вспомогательные DTO для парсинга JSON
    @Data private static class QuestChainJsonDto {
        private String chainKey;
        private String title;
        private String description;
        private String iconUrl;
        private QuestChainType type;
        private List<QuestStepJsonDto> steps;
    }

    @Data private static class QuestStepJsonDto {
        private String questKey;
        private int questOrder;
        private String title;
        private String npcName;
        private String npcImageUrl;
        private String storyText;
        private String objective;
        private RewardsJsonDto rewards;
        private String buttonText;
        private String battleLocationId;
    }

    @Data private static class RewardsJsonDto {
        private long gold;
        private long experience;
        private long wood;
        private long stone;
        private List<ItemRewardJsonDto> items;
    }

    @Data private static class ItemRewardJsonDto {
        private String itemKey;
        private String name;
        private String imageUrl;
        private int quantity;
    }
}