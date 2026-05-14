package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.DeadPirateDto;
import org.example.piratelegacy.auth.dto.TavernUnitDto;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShamanService {

    private static final long HERO_INSTANT_CRYSTALS = 300L;

    private final UnitRepository unitRepository;
    private final UserResourcesRepository resourcesRepository;
    private final UserProgressService userProgressService;
    private final TavernService tavernService;

    /**
     * Список мёртвых пиратов с ценами воскрешения из конфига.
     */
    public List<DeadPirateDto> getDeadPirates(User user) {
        return unitRepository.findByOwnerId(user.getId()).stream()
                .filter(u -> !u.isAlive() && !Boolean.TRUE.equals(u.getIsMainHero()))
                .map(u -> {
                    TavernUnitDto config = tavernService.getUnitConfig(u.getUnitTypeKey());
                    long reviveGold     = config != null ? config.getReviveGoldCost()     : 500L;
                    long reviveCrystals = config != null ? config.getReviveCrystalsCost() : 5L;
                    return DeadPirateDto.builder()
                            .id(u.getId())
                            .name(u.getName())
                            .level(u.getLevel())
                            .unitTypeKey(u.getUnitTypeKey())
                            .reviveGoldCost(reviveGold)
                            .reviveCrystalsCost(reviveCrystals)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Воскресить пирата у шамана.
     * paymentType: "GOLD" или "CRYSTALS"
     */
    @Transactional
    public void revivePirate(User user, Long unitId, String paymentType) {
        Unit unit = unitRepository.findByIdAndOwnerId(unitId, user.getId())
                .orElseThrow(() -> new ApiException("Юнит не найден.", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(unit.getIsMainHero())) {
            throw new ApiException("Главный герой воскрешается не у шамана.", HttpStatus.BAD_REQUEST);
        }

        if (unit.isAlive()) {
            throw new ApiException("Этот пират жив и не нуждается в воскрешении.", HttpStatus.BAD_REQUEST);
        }

        // Берём цены из конфига по типу юнита
        TavernUnitDto config = tavernService.getUnitConfig(unit.getUnitTypeKey());
        long reviveGold     = config != null ? config.getReviveGoldCost()     : 500L;
        long reviveCrystals = config != null ? config.getReviveCrystalsCost() : 5L;

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        if ("CRYSTALS".equalsIgnoreCase(paymentType)) {
            if (resources.getCrystals() < reviveCrystals) {
                throw new ApiException("Недостаточно кристаллов. Нужно: " + reviveCrystals, HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), 0, 0, 0, -reviveCrystals);
        } else {
            if (resources.getGold() < reviveGold) {
                throw new ApiException("Недостаточно золота. Нужно: " + reviveGold, HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), -reviveGold, 0, 0, 0);
        }

        unit.setAlive(true);
        unitRepository.save(unit);
        log.info("Pirate '{}' (id={}) revived for user {}", unit.getName(), unit.getId(), user.getId());

        // Триггерим квест raise_the_dead если активен
        userProgressService.handleAction(user, QuestTriggerAction.REVIVE_UNIT);
    }

    /**
     * Моментальное воскрешение главного героя за кристаллы.
     */
    @Transactional
    public void reviveHeroInstant(User user) {
        Unit hero = unitRepository.findByOwnerIdAndIsMainHeroTrue(user.getId())
                .orElseThrow(() -> new ApiException("Главный герой не найден.", HttpStatus.NOT_FOUND));

        if (hero.isAlive()) {
            throw new ApiException("Герой жив и не нуждается в воскрешении.", HttpStatus.BAD_REQUEST);
        }

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        if (resources.getCrystals() < HERO_INSTANT_CRYSTALS) {
            throw new ApiException("Недостаточно кристаллов. Нужно: " + HERO_INSTANT_CRYSTALS, HttpStatus.BAD_REQUEST);
        }

        resourcesRepository.addResources(user.getId(), 0, 0, 0, -HERO_INSTANT_CRYSTALS);
        hero.setAlive(true);
        hero.setRecoveryEndsAt(null);
        unitRepository.save(hero);
        log.info("Hero '{}' instantly revived for user {}", hero.getName(), user.getId());
    }

    /**
     * Проверяет истёк ли таймер восстановления героя и воскрешает автоматически.
     * Вызывается при входе игрока в игру.
     */
    @Transactional
    public void checkHeroRecovery(User user) {
        unitRepository.findByOwnerIdAndIsMainHeroTrue(user.getId()).ifPresent(hero -> {
            if (!hero.isAlive()
                    && hero.getRecoveryEndsAt() != null
                    && LocalDateTime.now().isAfter(hero.getRecoveryEndsAt())) {
                hero.setAlive(true);
                hero.setRecoveryEndsAt(null);
                unitRepository.save(hero);
                log.info("Hero '{}' auto-recovered for user {}", hero.getName(), user.getId());
            }
        });
    }
}