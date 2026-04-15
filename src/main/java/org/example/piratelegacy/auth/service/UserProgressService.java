package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.JournalDto;
import org.example.piratelegacy.auth.dto.MainIslandDto;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserQuest;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UserQuestRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final UserQuestRepository userQuestRepository;
    private final QuestService questService;
    private final UserResourcesService userResourcesService;
    private final PlayerLevelService playerLevelService;
    private final UserInventoryService userInventoryService;
    private final JournalService journalService;

    @Transactional
    @CacheEvict(value = "journals", key = "#user.id")
    public void completeQuest(User user, String questKey) {
        Quest quest = questService.getQuestByKey(questKey);

        boolean alreadyCompleted = userQuestRepository.existsByUserIdAndQuestId(user.getId(), quest.getId());
        if (alreadyCompleted) {
            log.warn("User {} attempted to complete already completed quest '{}'", user.getId(), questKey);
            throw new IllegalStateException("Квест '" + questKey + "' уже выполнен.");
        }

        JournalDto journal = journalService.getJournalForUser(user);
        boolean isCurrentQuest = Stream
                .concat(journal.getStorylineChains().stream(), journal.getAdventureChains().stream())
                .filter(chain -> chain.getCurrentQuest() != null)
                .anyMatch(chain -> chain.getCurrentQuest().getQuestKey().equals(questKey));

        if (!isCurrentQuest) {
            log.warn("User {} attempted to complete non-active quest '{}'", user.getId(), questKey);
            throw new ApiException("Этот квест не является вашим текущим активным заданием.", HttpStatus.FORBIDDEN);
        }

        userQuestRepository.save(UserQuest.builder()
                .user(user)
                .quest(quest)
                .isCompleted(true)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build());
        log.info("User {} completed quest '{}'", user.getId(), questKey);

        // 1. Ресурсы — всегда
        userResourcesService.addResources(user,
                quest.getGoldReward(),
                quest.getWoodReward(),
                quest.getStoneReward(),
                quest.getCrystalsReward());

        // 2. Опыт острова — всегда (и за бой, и за квест без боя)
        //    Опыт герою и юнитам НЕ здесь — это делает BattleRewardService после боя
        if (quest.getExpReward() != null && quest.getExpReward() > 0) {
            playerLevelService.addExperience(user, quest.getExpReward());
        }

        // 3. Предметы — всегда
        if (quest.getItemRewards() != null && !quest.getItemRewards().isEmpty()) {
            userInventoryService.addItemsToInventory(user, quest.getItemRewards());
        }
    }

    public MainIslandDto getMainIslandData(User user) {
        UserResourcesDto resourcesDto = userResourcesService.getResources(user);
        JournalDto journalDto = journalService.getJournalForUser(user);
        return new MainIslandDto(resourcesDto, journalDto);
    }
}