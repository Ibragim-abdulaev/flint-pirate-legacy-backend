package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.QuestJsonDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.QuestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestInitializationService implements CommandLineRunner {

    private final QuestRepository questRepository;
    private final ObjectMapper objectMapper;

    @Value("${game.quests.init-from-json:true}")
    private boolean initFromJson;

    @Value("${game.quests.json-file:quests.json}")
    private String questsJsonFile;

    @Value("${game.quests.force-reload:false}")
    private boolean forceReload;

    @Override
    public void run(String... args) throws Exception {
        if (!initFromJson) {
            log.info("Quest initialization from JSON is disabled");
            return;
        }

        if (forceReload) {
            log.info("Force reloading quests from JSON...");
            questRepository.deleteAll();
        }

        if (questRepository.count() == 0) {
            log.info("Initializing quests from JSON file: {}", questsJsonFile);
            initializeQuestsFromJson();
        } else {
            log.info("Quests already exist in database, skipping initialization");
        }
    }

    @Transactional
    public void initializeQuestsFromJson() {
        try {
            Resource resource = new ClassPathResource(questsJsonFile);

            if (!resource.exists()) {
                log.warn("Quest JSON file not found: {}, skipping quest initialization", questsJsonFile);
                return;
            }

            InputStream inputStream = resource.getInputStream();

            List<QuestJsonDto> questDtos = objectMapper.readValue(inputStream, new TypeReference<>() {
            });

            if (questDtos.isEmpty()) {
                log.warn("No quests found in JSON file: {}", questsJsonFile);
                return;
            }

            List<Quest> quests = questDtos.stream()
                    .map(dto -> Quest.builder()
                            .title(dto.getTitle())
                            .description(dto.getDescription())
                            .npcName(dto.getNpcName())
                            .npcImageUrl(dto.getNpcImageUrl())
                            .goldReward(dto.getGoldReward() != null ? dto.getGoldReward() : 0L)
                            .expReward(dto.getExpReward() != null ? dto.getExpReward() : 0L)
                            .buttonText(dto.getButtonText() != null ? dto.getButtonText() : "В путь")
                            .questOrder(dto.getQuestOrder())
                            .isActive(dto.isActive())
                            .build())
                    .collect(Collectors.toList());

            questRepository.saveAll(quests);
            log.info("Successfully initialized {} quests from JSON", quests.size());

        } catch (Exception e) {
            log.error("Failed to load quests from JSON file: {}", questsJsonFile, e);
            log.info("Quest initialization failed, application will continue without default quests");
        }
    }

    @Transactional
    public void initializeQuestsForUser(User user) {
        // При создании персонажа квесты уже должны быть в базе
        // Этот метод пока ничего не делает, но может быть расширен позже
        // для персональных квестов пользователя
    }

    /**
     * Метод для ручной перезагрузки квестов (может быть вызван через admin API)
     */
    @Transactional
    public void reloadQuestsFromJson() {
        log.info("Manual quest reload requested");
        questRepository.deleteAll();
        initializeQuestsFromJson();
    }
}
