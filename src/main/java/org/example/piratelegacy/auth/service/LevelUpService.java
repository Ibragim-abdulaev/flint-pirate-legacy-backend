package org.example.piratelegacy.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.Unit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Логика повышения уровня юнитов и героя.
 *
 * XP хранится суммарно (не вычитается при level up).
 * Уровень пересчитывается из суммарного XP.
 *
 * Формула: для уровня N нужно суммарно:
 * 1000 + 2000 + ... + 1000*(N-1) XP
 * Уровень 2 = 1000, уровень 3 = 3000, уровень 4 = 6000 и т.д.
 *
 * Прирост статов за каждый уровень:
 *   HP    +10% от базового 1-го уровня
 *   Атака +8%  от базового 1-го уровня
 *   Броня +5%  от базового 1-го уровня
 */
@Slf4j
@Service
public class LevelUpService {

    public static final int MAX_LEVEL = 30;

    private static final double HP_GAIN     = 0.10;
    private static final double ATTACK_GAIN = 0.08;
    private static final double ARMOR_GAIN  = 0.05;

    /**
     * Суммарный XP необходимый чтобы достичь указанного уровня с нуля.
     */
    public static long totalXpForLevel(int level) {
        if (level <= 1) return 0;
        long total = 0;
        for (int i = 1; i < level; i++) {
            total += 1000L * i;
        }
        return total;
    }

    /**
     * Вычисляет уровень по суммарному XP.
     */
    public static int levelFromTotalXp(long totalXp) {
        int level = 1;
        while (level < MAX_LEVEL && totalXp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /**
     * Сколько XP осталось до следующего уровня.
     */
    public static long xpToNextLevel(long totalXp) {
        int current = levelFromTotalXp(totalXp);
        if (current >= MAX_LEVEL) return 0;
        return totalXpForLevel(current + 1) - totalXp;
    }

    /**
     * Проверяет нужен ли level up и применяет его.
     * XP не вычитается — уровень пересчитывается из суммарного XP.
     * Изменяет unit напрямую — сохранение делается снаружи.
     *
     * @return список новых уровней которых достиг юнит (пустой если не было level up)
     */
    public List<Integer> checkAndApplyLevelUps(Unit unit) {
        List<Integer> newLevels = new ArrayList<>();

        int newLevel = levelFromTotalXp(unit.getExperience());

        while (unit.getLevel() < newLevel && unit.getLevel() < MAX_LEVEL) {
            int oldLevel = unit.getLevel();
            unit.setLevel(oldLevel + 1);

            int hpGain     = Math.max(1, (int) Math.round(unit.getBaseHp()        * HP_GAIN));
            int minAtkGain = Math.max(1, (int) Math.round(unit.getBaseMinAttack() * ATTACK_GAIN));
            int maxAtkGain = Math.max(1, (int) Math.round(unit.getBaseMaxAttack() * ATTACK_GAIN));
            int armorGain  = Math.max(1, (int) Math.round(unit.getBaseArmor()     * ARMOR_GAIN));

            unit.setBaseHp(unit.getBaseHp() + hpGain);
            unit.setBaseMinAttack(unit.getBaseMinAttack() + minAtkGain);
            unit.setBaseMaxAttack(unit.getBaseMaxAttack() + maxAtkGain);
            unit.setBaseArmor(unit.getBaseArmor() + armorGain);

            log.info("Unit '{}' (id={}) leveled up {} → {}. HP+{}, ATK+{}/{}, ARM+{}",
                    unit.getName(), unit.getId(), oldLevel, unit.getLevel(),
                    hpGain, minAtkGain, maxAtkGain, armorGain);

            newLevels.add(unit.getLevel());
        }

        return newLevels;
    }
}