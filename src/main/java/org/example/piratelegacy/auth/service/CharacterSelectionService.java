package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.request.CharacterSelectionRequest;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.CharacterType;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.exception.ResourceNotFoundException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterSelectionService {

    private final UnitRepository unitRepository;
    private final GameConfigService gameConfigService;

    @Transactional
    public Unit selectCharacter(User user, CharacterSelectionRequest request) {
        if (unitRepository.existsByOwnerId(user.getId())) {
            throw new IllegalStateException("У пользователя уже есть персонаж!");
        }

        CharacterType characterType;
        try {
            characterType = CharacterType.valueOf(request.getCharacterType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Неверный тип персонажа: " + request.getCharacterType(), HttpStatus.BAD_REQUEST);
        }

        GameConfigService.CharacterStats stats = gameConfigService.getCharacterStats(characterType);
        if (stats == null) {
            throw new ApiException("Характеристики для персонажа не найдены: " + characterType, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Unit mainHeroUnit = Unit.builder()
                .owner(user)
                .unitTypeKey(stats.getCharacterType().name()) // Например, "BARBARIAN"
                .name(request.getName().trim())
                .level(1)
                .experience(0L)
                .baseHp(stats.getBaseHp())
                .baseMinAttack(stats.getMinAttack())
                .baseMaxAttack(stats.getMaxAttack())
                .baseArmor(stats.getBaseArmor())
                .isMainHero(true) // Указываем, что это главный герой
                .build();

        return unitRepository.save(mainHeroUnit);
    }

    @Transactional(readOnly = true)
    public boolean userHasCharacter(User user) {
        return unitRepository.existsByOwnerIdAndIsMainHeroTrue(user.getId());
    }

    @Transactional(readOnly = true)
    public Unit getUserCharacter(User user) {
        return unitRepository.findByOwnerIdAndIsMainHeroTrue(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("У пользователя нет главного героя"));
    }
}