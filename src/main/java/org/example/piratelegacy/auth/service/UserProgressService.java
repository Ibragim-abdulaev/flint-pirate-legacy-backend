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

/**
 * Сервис для управления прогрессом пользователя в игре,
 * в первую очередь - для выполнения квестов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final UserQuestRepository userQuestRepository;
    private final QuestService questService;
    private final UserResourcesService userResourcesService;
    private final UserStatsService userStatsService;
    private final UserInventoryService userInventoryService;
    private final JournalService journalService;

    /**
     * Завершает квест для пользователя, проверяя, является ли он текущим активным,
     * и начисляет все награды (ресурсы, опыт, предметы).
     *
     * @param user     Пользователь, завершающий квест.
     * @param questKey Уникальный строковый ключ квеста.
     */
    @Transactional
    @CacheEvict(value = "journals", key = "#user.id")
    public void completeQuest(User user, String questKey) {
        Quest questToComplete = questService.getQuestByKey(questKey);

        boolean alreadyCompleted = userQuestRepository.existsByUserIdAndQuestId(user.getId(), questToComplete.getId());
        if (alreadyCompleted) {
            log.warn("User {} attempted to complete already completed quest '{}'", user.getId(), questKey);
            throw new IllegalStateException("Квест '" + questKey + "' уже выполнен.");
        }

        JournalDto journal = journalService.getJournalForUser(user);
        boolean isCurrentQuest = Stream.concat(journal.getStorylineChains().stream(), journal.getAdventureChains().stream())
                .filter(chain -> chain.getCurrentQuest() != null)
                .anyMatch(chain -> chain.getCurrentQuest().getQuestKey().equals(questKey));

        if (!isCurrentQuest) {
            log.warn("User {} attempted to complete non-active quest '{}'", user.getId(), questKey);
            throw new ApiException("Этот квест не является вашим текущим активным заданием.", HttpStatus.FORBIDDEN);
        }

        UserQuest userQuest = UserQuest.builder()
                .user(user)
                .quest(questToComplete)
                .isCompleted(true)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        userQuestRepository.save(userQuest);
        log.info("User {} successfully completed quest '{}'", user.getId(), questKey);

        userResourcesService.addResources(user,
                questToComplete.getGoldReward(),
                questToComplete.getWoodReward(),
                questToComplete.getStoneReward(),
                questToComplete.getCrystalsReward());

        if (questToComplete.getExpReward() != null && questToComplete.getExpReward() > 0) {
            userStatsService.addExperienceToMainHero(user, questToComplete.getExpReward());
        }

        if (questToComplete.getItemRewards() != null && !questToComplete.getItemRewards().isEmpty()) {
            userInventoryService.addItemsToInventory(user, questToComplete.getItemRewards());
        }
    }

    public MainIslandDto getMainIslandData(User user) {
        UserResourcesDto resourcesDto = userResourcesService.getResources(user);
        JournalDto journalDto = journalService.getJournalForUser(user);

        return new MainIslandDto(resourcesDto, journalDto);
    }
}