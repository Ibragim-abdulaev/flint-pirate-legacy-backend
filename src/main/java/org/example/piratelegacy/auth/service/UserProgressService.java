package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.JournalDto;
import org.example.piratelegacy.auth.dto.MainIslandDto;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserQuest;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UserQuestRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
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

    /**
     * Завершает квест вручную (для квестов без triggerAction).
     * Например: победить в бою, нажать кнопку "Забрать".
     */
    @Transactional
    @CacheEvict(value = "journals", key = "#user.id")
    public void completeQuest(User user, String questKey) {
        Quest quest = questService.getQuestByKey(questKey);

        boolean alreadyCompleted = userQuestRepository.existsByUserIdAndQuestId(user.getId(), quest.getId());
        if (alreadyCompleted) {
            log.warn("User {} attempted to complete already completed quest '{}'", user.getId(), questKey);
            throw new IllegalStateException("Квест '" + questKey + "' уже выполнен.");
        }

        assertIsCurrentQuest(user, questKey);
        saveAndReward(user, quest);
    }

    /**
     * Вызывается когда игрок совершил какое-то действие (HIRE_UNIT, BUILD_SHIP и т.д.).
     * Находит текущий активный квест с таким triggerAction и увеличивает его прогресс.
     * Если прогресс достиг requiredCount — завершает квест и начисляет награды.
     */
    @Transactional
    @CacheEvict(value = "journals", key = "#user.id")
    public void handleAction(User user, QuestTriggerAction action) {
        JournalDto journal = journalService.getJournalForUser(user);

        // Ищем текущий активный квест с таким triggerAction
        Quest activeQuest = Stream
                .concat(journal.getStorylineChains().stream(), journal.getAdventureChains().stream())
                .filter(chain -> chain.getCurrentQuest() != null)
                .map(chain -> questService.getQuestByKey(chain.getCurrentQuest().getQuestKey()))
                .filter(quest -> action == quest.getTriggerAction())
                .findFirst()
                .orElse(null);

        if (activeQuest == null) {
            log.debug("No active quest found for action {} for user {}", action, user.getId());
            return;
        }

        // Ищем или создаём запись прогресса
        Optional<UserQuest> existing = userQuestRepository
                .findByUserIdAndQuestId(user.getId(), activeQuest.getId());

        UserQuest userQuest = existing.orElseGet(() -> UserQuest.builder()
                .user(user)
                .quest(activeQuest)
                .isCompleted(false)
                .progress(0)
                .startedAt(LocalDateTime.now())
                .build());

        userQuest.setProgress(userQuest.getProgress() + 1);
        log.info("Quest '{}' progress for user {}: {}/{}",
                activeQuest.getQuestKey(), user.getId(),
                userQuest.getProgress(), activeQuest.getRequiredCount());

        if (userQuest.getProgress() >= activeQuest.getRequiredCount()) {
            userQuest.setCompleted(true);
            userQuest.setCompletedAt(LocalDateTime.now());
            userQuestRepository.save(userQuest);
            log.info("User {} completed quest '{}' via action {}",
                    user.getId(), activeQuest.getQuestKey(), action);
            rewardUser(user, activeQuest);
        } else {
            userQuestRepository.save(userQuest);
        }
    }

    public MainIslandDto getMainIslandData(User user) {
        UserResourcesDto resourcesDto = userResourcesService.getResources(user);
        JournalDto journalDto = journalService.getJournalForUser(user);
        return new MainIslandDto(resourcesDto, journalDto);
    }

    // --- private ---

    private void assertIsCurrentQuest(User user, String questKey) {
        if (!isCurrentQuest(user, questKey)) {
            log.warn("User {} attempted to complete non-active quest '{}'", user.getId(), questKey);
            throw new ApiException("Этот квест не является вашим текущим активным заданием.", HttpStatus.FORBIDDEN);
        }
    }

    private boolean isCurrentQuest(User user, String questKey) {
        JournalDto journal = journalService.getJournalForUser(user);
        return Stream.concat(journal.getStorylineChains().stream(), journal.getAdventureChains().stream())
                .filter(chain -> chain.getCurrentQuest() != null)
                .anyMatch(chain -> chain.getCurrentQuest().getQuestKey().equals(questKey));
    }

    private void saveAndReward(User user, Quest quest) {
        userQuestRepository.save(UserQuest.builder()
                .user(user)
                .quest(quest)
                .isCompleted(true)
                .progress(quest.getRequiredCount())
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build());
        log.info("User {} completed quest '{}'", user.getId(), quest.getQuestKey());
        rewardUser(user, quest);
    }

    private void rewardUser(User user, Quest quest) {
        userResourcesService.addResources(user,
                quest.getGoldReward(),
                quest.getWoodReward(),
                quest.getStoneReward(),
                quest.getCrystalsReward());

        if (quest.getExpReward() != null && quest.getExpReward() > 0) {
            playerLevelService.addExperience(user, quest.getExpReward());
        }

        if (quest.getItemRewards() != null && !quest.getItemRewards().isEmpty()) {
            userInventoryService.addItemsToInventory(user, quest.getItemRewards());
        }
    }
}