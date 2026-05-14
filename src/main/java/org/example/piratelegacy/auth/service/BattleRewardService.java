package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.dto.BattleResultDto;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleRewardService {

    private static final int HERO_RECOVERY_HOURS = 3;

    private final UnitRepository unitRepository;
    private final LevelUpService levelUpService;
    private final PlayerLevelService playerLevelService;
    private final UserProgressService userProgressService;

    @Transactional
    public void processBattleRewards(User user, BattleResultDto result, List<BattlePirateDto> participants) {
        boolean playerWon = result.getWinnerTeam() == TeamType.ALLY;
        long expReward = result.getRewards() != null ? result.getRewards().getExperience() : 0;

        Set<Long> realUnitIds = participants.stream()
                .filter(p -> p.getTeam() == TeamType.ALLY)
                .map(p -> {
                    try { return Long.parseLong(p.getId()); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Показательный бой — только остров получает опыт
        if (realUnitIds.isEmpty()) {
            log.info("Showcase battle for user {} — no real units to reward", user.getId());
            if (playerWon && expReward > 0) {
                playerLevelService.addExperience(user, expReward);
            }
            return;
        }

        Set<String> deadIds = result.getYourLossesIds() != null
                ? new HashSet<>(result.getYourLossesIds())
                : Set.of();

        List<Unit> units = unitRepository.findByOwnerIdAndIdIn(user.getId(), realUnitIds);

        for (Unit unit : units) {
            boolean died = deadIds.contains(String.valueOf(unit.getId()));

            if (died) {
                unit.setAlive(false);
                if (Boolean.TRUE.equals(unit.getIsMainHero())) {
                    unit.setRecoveryEndsAt(LocalDateTime.now().plusHours(HERO_RECOVERY_HOURS));
                    log.info("Hero '{}' died. Recovery ends at: {}", unit.getName(), unit.getRecoveryEndsAt());
                } else {
                    log.info("Unit '{}' (id={}) died in battle.", unit.getName(), unit.getId());
                }
                unitRepository.save(unit);
                continue;
            }

            if (playerWon && expReward > 0) {
                unit.setExperience(unit.getExperience() + expReward);
                levelUpService.checkAndApplyLevelUps(unit);
                unitRepository.save(unit);
                log.info("Unit '{}' (id={}) gained {} XP.", unit.getName(), unit.getId(), expReward);
            }
        }

        if (playerWon && expReward > 0) {
            playerLevelService.addExperience(user, expReward);
        }

        // Триггерим квест win_the_battle при победе
        if (playerWon) {
            userProgressService.handleAction(user, QuestTriggerAction.WIN_BATTLE);
        }
    }
}