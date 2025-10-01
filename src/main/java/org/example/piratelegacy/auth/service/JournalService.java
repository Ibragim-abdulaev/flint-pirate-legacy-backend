package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.ItemRewardDto;
import org.example.piratelegacy.auth.dto.JournalDto;
import org.example.piratelegacy.auth.dto.QuestChainStatusDto;
import org.example.piratelegacy.auth.dto.QuestDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.QuestChain;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserQuest;
import org.example.piratelegacy.auth.entity.enums.QuestChainType;
import org.example.piratelegacy.auth.repository.QuestChainRepository;
import org.example.piratelegacy.auth.repository.UserQuestRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final QuestChainRepository questChainRepository;
    private final UserQuestRepository userQuestRepository;
    private final QuestService questService;

    @Transactional(readOnly = true)
    @Cacheable(value = "journals", key = "#user.id")
    public JournalDto getJournalForUser(User user) {
        List<QuestChain> allChains = questChainRepository.findAll();
        List<UserQuest> userCompletedQuests = userQuestRepository.findByUserIdAndIsCompletedTrue(user.getId());

        Map<Long, List<UserQuest>> completedQuestsByChainId = userCompletedQuests.stream()
                .collect(Collectors.groupingBy(uq -> uq.getQuest().getQuestChain().getId()));

        List<QuestChain> sortedStoryline = allChains.stream()
                .filter(qc -> qc.getChainType() == QuestChainType.STORYLINE)
                .sorted(Comparator.comparing(QuestChain::getId))
                .collect(Collectors.toList());

        List<QuestChain> sortedAdventures = allChains.stream()
                .filter(qc -> qc.getChainType() == QuestChainType.ADVENTURE)
                .sorted(Comparator.comparing(QuestChain::getId))
                .collect(Collectors.toList());

        List<QuestChainStatusDto> storylineChains = buildChainStatusList(sortedStoryline, user, completedQuestsByChainId);
        List<QuestChainStatusDto> adventureChains = buildChainStatusList(sortedAdventures, user, completedQuestsByChainId);

        return JournalDto.builder()
                .storylineChains(storylineChains)
                .adventureChains(adventureChains)
                .build();
    }

    private List<QuestChainStatusDto> buildChainStatusList(List<QuestChain> sortedChains, User user, Map<Long, List<UserQuest>> completedMap) {
        return sortedChains.stream()
                .map(chain -> {
                    QuestChain previousChain = getPreviousChain(chain, sortedChains);
                    return buildChainStatus(chain, user, completedMap, previousChain);
                })
                .collect(Collectors.toList());
    }

    private QuestChain getPreviousChain(QuestChain currentChain, List<QuestChain> sortedChains) {
        int currentIndex = sortedChains.indexOf(currentChain);
        if (currentIndex > 0) {
            return sortedChains.get(currentIndex - 1);
        }
        return null;
    }

    private QuestChainStatusDto buildChainStatus(QuestChain chain, User user, Map<Long, List<UserQuest>> completedMap, QuestChain previousChain) {
        List<UserQuest> completedInThisChain = completedMap.getOrDefault(chain.getId(), List.of());
        int completedSteps = completedInThisChain.size();
        int totalSteps = chain.getQuests().size();
        boolean isCompleted = totalSteps > 0 && completedSteps == totalSteps;

        boolean isAvailable = isChainAvailable(chain, completedMap, previousChain);

        Quest currentQuestEntity = (isCompleted || !isAvailable) ? null : questService.findNextQuestInChain(user, chain, completedSteps + 1);
        QuestDto currentQuestDto = mapQuestToDto(currentQuestEntity);

        return QuestChainStatusDto.builder()
                .chainKey(chain.getChainKey())
                .title(chain.getTitle())
                .iconUrl(chain.getIconUrl())
                .completedSteps(completedSteps)
                .totalSteps(totalSteps)
                .isAvailable(isAvailable)
                .isCompleted(isCompleted)
                .currentQuest(currentQuestDto)
                .build();
    }

    private boolean isChainAvailable(QuestChain chain, Map<Long, List<UserQuest>> completedMap, QuestChain previousChain) {
        if (previousChain == null) {
            return true;
        }

        List<UserQuest> completedInPrevious = completedMap.getOrDefault(previousChain.getId(), List.of());
        int totalInPrevious = previousChain.getQuests().size();

        return totalInPrevious > 0 && completedInPrevious.size() == totalInPrevious;
    }

    private QuestDto mapQuestToDto(Quest quest) {
        if (quest == null) return null;
        List<ItemRewardDto> itemRewards = quest.getItemRewards().stream()
                .map(reward -> new ItemRewardDto(
                        reward.getItem().getItemKey(),
                        reward.getItem().getName(),
                        reward.getItem().getImageUrl(),
                        reward.getQuantity()))
                .collect(Collectors.toList());
        return new QuestDto(
                quest.getQuestKey(), quest.getTitle(), quest.getNpcName(), quest.getNpcImageUrl(),
                quest.getStoryText(), quest.getObjective(), quest.getGoldReward(),
                quest.getExpReward(), itemRewards, quest.getButtonText(), quest.getBattleLocationId());
    }
}