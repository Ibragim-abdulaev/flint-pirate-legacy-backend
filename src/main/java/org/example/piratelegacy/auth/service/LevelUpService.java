package org.example.piratelegacy.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.Unit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Сервис повышения уровня юнитов.
 *
 * Формула XP: xpRequired(level) = 1000 * level
 * Например: 1→2 = 1000 XP, 2→3 = 2000 XP, 3→4 = 3000 XP
 *
 * Прирост статов за уровень (процент от базовых характеристик 1-го уровня):
 *   HP         +10%
 *   minAttack  +8%
 *   maxAttack  +8%
 *   armor      +5%
 */
@Slf4j
@Service
public class LevelUpService {

    private static final int MAX_LEVEL = 30;

    // Множители прироста за уровень
    private static final double HP_GAIN_PER_LEVEL      = 0.10;
    private static final double ATTACK_GAIN_PER_LEVEL  = 0.08;
    private static final double ARMOR_GAIN_PER_LEVEL   = 0.05;

    /**
     * Вычисляет количество XP, необходимое для перехода НА указанный уровень.
     * Например, для достижения уровня 2 нужно набрать 1000 XP.
     */
    public long xpRequiredForLevel(int level) {
        if (level <= 1) return 0;
        return 1000L * (level - 1);
    }

    /**
     * Суммарный XP, нужный с нуля чтобы достичь указанного уровня.
     * Например, чтобы быть на уровне 3 нужно: 1000 + 2000 = 3000 XP суммарно.
     */
    public long totalXpRequiredForLevel(int level) {
        if (level <= 1) return 0;
        long total = 0;
        for (int i = 2; i <= level; i++) {
            total += xpRequiredForLevel(i);
        }
        return total;
    }

    /**
     * XP до следующего уровня от текущего накопленного XP.
     */
    public long xpForNextLevel(Unit unit) {
        if (unit.getLevel() >= MAX_LEVEL) return 0;
        long totalNeeded = totalXpRequiredForLevel(unit.getLevel() + 1);
        return Math.max(0, totalNeeded - unit.getExperience());
    }

    /**
     * Проверяет, нужно ли юниту повысить уровень, и повышает — возможно несколько раз
     * (если за один бой набрали много XP).
     *
     * @param unit Юнит, которому начислили XP
     * @return Список результатов level up (пустой если уровень не повысился)
     */
    public List<LevelUpResult> checkAndApplyLevelUps(Unit unit) {
        List<LevelUpResult> results = new ArrayList<>();

        while (unit.getLevel() < MAX_LEVEL) {
            long totalNeededForNext = totalXpRequiredForLevel(unit.getLevel() + 1);
            if (unit.getExperience() < totalNeededForNext) {
                break;
            }

            // Повышаем уровень
            int oldLevel = unit.getLevel();
            int newLevel = oldLevel + 1;

            // Вычисляем прирост статов на основе базовых характеристик 1-го уровня
            // (чтобы прирост был стабильным и предсказуемым)
            int hpGain       = Math.max(1, (int) Math.round(unit.getBaseHp()        * HP_GAIN_PER_LEVEL));
            int minAtkGain   = Math.max(1, (int) Math.round(unit.getBaseMinAttack() * ATTACK_GAIN_PER_LEVEL));
            int maxAtkGain   = Math.max(1, (int) Math.round(unit.getBaseMaxAttack() * ATTACK_GAIN_PER_LEVEL));
            int armorGain    = Math.max(1, (int) Math.round(unit.getBaseArmor()     * ARMOR_GAIN_PER_LEVEL));

            // Применяем прирост к базовым статам
            unit.setLevel(newLevel);
            unit.setBaseHp(unit.getBaseHp() + hpGain);
            unit.setBaseMinAttack(unit.getBaseMinAttack() + minAtkGain);
            unit.setBaseMaxAttack(unit.getBaseMaxAttack() + maxAtkGain);
            unit.setBaseArmor(unit.getBaseArmor() + armorGain);

            log.info("Unit '{}' (id={}) leveled up: {} → {}. Gains: HP+{}, ATK+{}-{}, ARM+{}",
                    unit.getName(), unit.getId(), oldLevel, newLevel,
                    hpGain, minAtkGain, maxAtkGain, armorGain);

            results.add(LevelUpResult.builder()
                    .newLevel(newLevel)
                    .hpGain(hpGain)
                    .minAttackGain(minAtkGain)
                    .maxAttackGain(maxAtkGain)
                    .armorGain(armorGain)
                    .currentExperience(unit.getExperience())
                    .experienceForNextLevel(xpForNextLevel(unit))
                    .build());
        }

        return results;
    }
}
