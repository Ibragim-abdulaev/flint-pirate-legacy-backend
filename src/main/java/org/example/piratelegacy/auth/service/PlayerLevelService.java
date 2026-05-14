package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Логика уровня острова (player_level / player_exp в таблице users).
 *
 * Использует ту же формулу XP что и LevelUpService.
 * player_exp только растёт — уровень пересчитывается из суммарного XP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerLevelService {

    private final UserRepository userRepository;

    /**
     * Сколько XP осталось до следующего уровня острова.
     */
    public static long xpToNextLevel(long totalXp) {
        return LevelUpService.xpToNextLevel(totalXp);
    }

    /**
     * Начисляет опыт острову и пересчитывает уровень.
     */
    @Transactional
    public void addExperience(User user, long expToAdd) {
        if (expToAdd <= 0) return;

        long newExp  = user.getPlayerExp() + expToAdd;
        int oldLevel = user.getPlayerLevel();
        int newLevel = LevelUpService.levelFromTotalXp(newExp);

        user.setPlayerExp(newExp);
        user.setPlayerLevel(newLevel);
        userRepository.save(user);

        if (newLevel > oldLevel) {
            log.info("Island of user {} reached level {}!", user.getId(), newLevel);
        }
        log.info("Island of user {} gained {} XP. Total: {}", user.getId(), expToAdd, newExp);
    }
}