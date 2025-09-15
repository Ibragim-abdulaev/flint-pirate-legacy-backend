package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.QuestJsonDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserQuest;
import org.example.piratelegacy.auth.repository.QuestRepository;
import org.example.piratelegacy.auth.repository.UserQuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserResourcesService userResourcesService;

    @Transactional(readOnly = true)
    public QuestJsonDto getCurrentQuest(User user) {
        // Проверяем, есть ли активный квест
        Optional<UserQuest> activeQuest = userQuestRepository.findByUserIdAndIsCompletedFalse(user.getId());

        if (activeQuest.isPresent()) {
            return toDto(activeQuest.get().getQuest());
        }

        // Если нет активного квеста, находим следующий
        long completedCount = userQuestRepository.countByUserIdAndIsCompletedTrue(user.getId());
        int nextOrder = (int) (completedCount + 1);

        Optional<Quest> nextQuest = questRepository.findByQuestOrderAndIsActiveTrue(nextOrder);

        if (nextQuest.isPresent()) {
            // Автоматически начинаем новый квест
            UserQuest userQuest = UserQuest.builder()
                    .user(user)
                    .quest(nextQuest.get())
                    .isCompleted(false)
                    .startedAt(LocalDateTime.now())
                    .build();
            userQuestRepository.save(userQuest);

            return toDto(nextQuest.get());
        }

        throw new RuntimeException("No available quests");
    }

    @Transactional
    public QuestJsonDto completeQuest(User user, Long questId) {
        UserQuest userQuest = userQuestRepository.findByUserIdAndQuestId(user.getId(), questId)
                .orElseThrow(() -> new RuntimeException("Quest not found"));

        if (userQuest.isCompleted()) {
            throw new RuntimeException("Quest already completed");
        }

        // Завершаем квест
        userQuest.setCompleted(true);
        userQuest.setCompletedAt(LocalDateTime.now());
        userQuestRepository.save(userQuest);

        // Выдаем награды
        Quest quest = userQuest.getQuest();
        if (quest.getGoldReward() > 0 || quest.getExpReward() > 0) {
            userResourcesService.addResources(user,
                    quest.getGoldReward() != null ? quest.getGoldReward() : 0,
                    0L, 0L);
        }

        // Пытаемся вернуть следующий квест, если он есть
        try {
            return getCurrentQuest(user);
        } catch (RuntimeException e) {
            // Если следующего квеста нет, возвращаем пустой ответ
            return new QuestJsonDto(
                    "Все квесты завершены",
                    "Вы выполнили все доступные квесты!",
                    "Старый Флинт",
                    "/images/old_flint.png",
                    0L, 0L,
                    "Завершено",
                    999,
                    false
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean hasActiveQuest(User user) {
        return userQuestRepository.findByUserIdAndIsCompletedFalse(user.getId()).isPresent();
    }

    /**
     * Инициализирует первый квест для нового пользователя после создания персонажа
     */
    @Transactional
    public void initializeFirstQuest(User user) {
        // Проверяем, нет ли уже квестов у пользователя
        long questCount = userQuestRepository.countByUserIdAndIsCompletedTrue(user.getId()) +
                (hasActiveQuest(user) ? 1 : 0);

        if (questCount > 0) {
            return; // У пользователя уже есть квесты
        }

        // Находим первый квест
        Optional<Quest> firstQuest = questRepository.findByQuestOrderAndIsActiveTrue(1);

        if (firstQuest.isPresent()) {
            // Создаем связь пользователя с первым квестом
            UserQuest userQuest = UserQuest.builder()
                    .user(user)
                    .quest(firstQuest.get())
                    .isCompleted(false)
                    .startedAt(LocalDateTime.now())
                    .build();
            userQuestRepository.save(userQuest);
        }
    }

    private QuestJsonDto toDto(Quest quest) {
        return new QuestJsonDto(
                quest.getTitle(),
                quest.getDescription(),
                quest.getNpcName(),
                quest.getNpcImageUrl(),
                quest.getGoldReward(),
                quest.getExpReward(),
                quest.getButtonText(),
                quest.getQuestOrder(),
                false
        );
    }
}