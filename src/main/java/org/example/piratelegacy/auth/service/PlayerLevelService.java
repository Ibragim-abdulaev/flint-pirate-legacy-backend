package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.LevelUpResult;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Управляет общим уровнем аккаунта игрока (player_level / player_exp в таблице users).
 *
 * Отличия от прокачки юнитов:
 * - player_exp только растёт (суммарный), никогда не вычитается
 * - player_level считается каждый раз из накопленного player_exp
 * - Формула: для уровня N нужно суммарно 1000 * N*(N-1)/2 XP
 *   (1→2: 1000, 1→3: 3000, 1→4: 6000 и т.д.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerLevelService {

    private static final int MAX_PLAYER_LEVEL = 50;

    private final UserRepository userRepository;

    /**
     * Суммарный XP необходимый чтобы достичь указанного уровня с нуля.
     * Например: уровень 3 = 1000 + 2000 = 3000 XP суммарно.
     */
    public static long totalXpForLevel(int level) {
        if (level <= 1) return 0;
        // Сумма 1000*1 + 1000*2 + ... + 1000*(level-1) = 1000 * level*(level-1)/2
        return 1000L * level * (level - 1) / 2;
    }

    /**
     * Вычисляет уровень игрока по суммарному накопленному XP.
     */
    public static int levelFromTotalXp(long totalXp) {
        int level = 1;
        while (level < MAX_PLAYER_LEVEL && totalXp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /**
     * XP до следующего уровня от текущего суммарного XP.
     */
    public static long xpToNextLevel(long totalXp) {
        int currentLevel = levelFromTotalXp(totalXp);
        if (currentLevel >= MAX_PLAYER_LEVEL) return 0;
        return totalXpForLevel(currentLevel + 1) - totalXp;
    }

    /**
     * Начисляет опыт аккаунту игрока и пересчитывает уровень.
     *
     * @return список событий повышения уровня (пустой если уровень не вырос)
     */
    @Transactional
    public List<LevelUpResult> addExperience(User user, long expToAdd) {
        if (expToAdd <= 0) return Collections.emptyList();

        int levelBefore = user.getPlayerLevel();
        long expBefore  = user.getPlayerExp();

        long newTotalExp = expBefore + expToAdd;
        int newLevel     = levelFromTotalXp(newTotalExp);

        user.setPlayerExp(newTotalExp);
        user.setPlayerLevel(newLevel);
        userRepository.save(user);

        log.info("Player {} gained {} XP. Total: {} → Level: {} → {}",
                user.getId(), expToAdd, newTotalExp, levelBefore, newLevel);

        if (newLevel <= levelBefore) return Collections.emptyList();

        // Собираем события для каждого повышенного уровня
        List<LevelUpResult> events = new ArrayList<>();
        for (int lvl = levelBefore + 1; lvl <= newLevel; lvl++) {
            events.add(LevelUpResult.builder()
                    .newLevel(lvl)
                    // У аккаунта нет статов — поля gains = 0
                    .hpGain(0)
                    .minAttackGain(0)
                    .maxAttackGain(0)
                    .armorGain(0)
                    .currentExperience(newTotalExp)
                    .experienceForNextLevel(xpToNextLevel(newTotalExp))
                    .build());

            log.info("Player {} reached account level {}!", user.getId(), lvl);
        }

        return events;
    }
}