package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.BattleResultDto;
import org.example.piratelegacy.auth.dto.LevelUpResult;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Начисляет опыт и обрабатывает смерти юнитов после боя.
 *
 * Правила:
 * - Герой и юниты получают опыт ТОЛЬКО если выжили в бою
 * - Погибшие помечаются isAlive=false
 * - Герою при гибели ставится recoveryEndsAt (таймер восстановления)
 * - Остров (player_exp) получает опыт всегда при победе
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleRewardService {

    // Через сколько часов восстанавливается главный герой
    private static final int HERO_RECOVERY_HOURS = 3;

    private final UnitRepository unitRepository;
    private final LevelUpService levelUpService;
    private final PlayerLevelService playerLevelService;

    @Transactional
    public BattleRewardResult processBattleRewards(User user, BattleResultDto battleResult) {
        boolean playerWon = battleResult.getWinnerTeam() == TeamType.ALLY;
        long expReward = battleResult.getRewards() != null ? battleResult.getRewards().getExperience() : 0;

        // ID погибших союзников из результата боя (это строки — ID юнитов в бою)
        Set<String> deadAlliesIds = battleResult.getYourLossesIds() != null
                ? battleResult.getYourLossesIds().stream().collect(Collectors.toSet())
                : Collections.emptySet();

        // Загружаем всех юнитов пользователя
        List<Unit> allUnits = unitRepository.findByOwnerId(user.getId());

        List<UnitExpResult> unitResults = new ArrayList<>();

        for (Unit unit : allUnits) {
            // Проверяем погиб ли юнит в этом бою
            // ID юнита в бою передаётся как строка, сравниваем с unit.getId()
            boolean diedInBattle = deadAlliesIds.contains(String.valueOf(unit.getId()));

            if (diedInBattle) {
                // Помечаем как мёртвого
                unit.setAlive(false);

                if (unit.getIsMainHero()) {
                    // Герою ставим таймер восстановления
                    unit.setRecoveryEndsAt(LocalDateTime.now().plusHours(HERO_RECOVERY_HOURS));
                    log.info("Hero '{}' died. Recovery ends at: {}", unit.getName(), unit.getRecoveryEndsAt());
                } else {
                    log.info("Unit '{}' (id={}) died in battle.", unit.getName(), unit.getId());
                }

                unitRepository.save(unit);
                unitResults.add(UnitExpResult.died(unit.getId(), unit.getName(), unit.isMainHero()));
                continue;
            }

            // Юнит выжил — начисляем опыт только если победа и есть награда
            if (playerWon && expReward > 0) {
                unit.setExperience(unit.getExperience() + expReward);
                List<LevelUpResult> levelUps = levelUpService.checkAndApplyLevelUps(unit);
                unitRepository.save(unit);
                log.info("Unit '{}' (id={}) gained {} XP. LevelUps: {}",
                        unit.getName(), unit.getId(), expReward, levelUps.size());
                unitResults.add(UnitExpResult.survived(unit.getId(), unit.getName(), unit.isMainHero(), expReward, levelUps));
            } else {
                unitResults.add(UnitExpResult.survived(unit.getId(), unit.getName(), unit.isMainHero(), 0, Collections.emptyList()));
            }
        }

        // Остров получает опыт всегда при победе
        List<LevelUpResult> playerLevelUps = Collections.emptyList();
        if (playerWon && expReward > 0) {
            playerLevelUps = playerLevelService.addExperience(user, expReward);
        }

        return new BattleRewardResult(unitResults, playerLevelUps);
    }

    // --- Вложенные классы результата ---

    public record BattleRewardResult(
            List<UnitExpResult> unitResults,
            List<LevelUpResult> playerLevelUps
    ) {}

    public record UnitExpResult(
            Long unitId,
            String unitName,
            boolean isHero,
            boolean died,
            long expGained,
            List<LevelUpResult> levelUps
    ) {
        static UnitExpResult died(Long id, String name, boolean isHero) {
            return new UnitExpResult(id, name, isHero, true, 0, Collections.emptyList());
        }

        static UnitExpResult survived(Long id, String name, boolean isHero, long exp, List<LevelUpResult> levelUps) {
            return new UnitExpResult(id, name, isHero, false, exp, levelUps);
        }
    }
}