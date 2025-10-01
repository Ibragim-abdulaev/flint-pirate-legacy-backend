package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.UnitProfileDto;
import org.example.piratelegacy.auth.dto.UnitSummaryDto;
import org.example.piratelegacy.auth.entity.Item;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "units")
public class UnitService {

    private final UnitRepository unitRepository;

    /**
     * Возвращает список всех юнитов в команде пользователя в кратком виде.
     */
    @Transactional(readOnly = true)
    @Cacheable(key = "'team_summary:' + #user.id")
    public List<UnitSummaryDto> getTeamSummary(User user) {
        return unitRepository.findByOwnerId(user.getId()).stream()
                .map(unit -> UnitSummaryDto.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .level(unit.getLevel())
                        // TODO: У юнита должно быть поле для URL портрета
                        .portraitImageUrl("/images/portraits/" + unit.getUnitTypeKey() + ".png")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "#unitId")
    public UnitProfileDto getUnitProfile(Long unitId, User user) {
        Unit unit = unitRepository.findByIdAndOwnerId(unitId, user.getId())
                .orElseThrow(() -> new IllegalStateException("Юнит не найден или не принадлежит вам."));

        Item weapon = unit.getEquippedWeapon() != null ? unit.getEquippedWeapon().getItem() : null;
        Item armor = unit.getEquippedArmor() != null ? unit.getEquippedArmor().getItem() : null;

        int bonusHp = (weapon != null ? weapon.getBonusHp() : 0) + (armor != null ? armor.getBonusHp() : 0);
        int bonusDamage = (weapon != null ? weapon.getBonusDamage() : 0) + (armor != null ? armor.getBonusDamage() : 0);
        int bonusArmor = (weapon != null ? weapon.getBonusArmor() : 0) + (armor != null ? armor.getBonusArmor() : 0);

        UnitProfileDto.Stat totalStats = UnitProfileDto.Stat.builder()
                .hp(unit.getBaseHp() + bonusHp)
                .minAttack(unit.getBaseMinAttack() + bonusDamage)
                .maxAttack(unit.getBaseMaxAttack() + bonusDamage)
                .armor(unit.getBaseArmor() + bonusArmor)
                .build();

        UnitProfileDto.EquipmentDto equipmentDto = UnitProfileDto.EquipmentDto.builder()
                .weapon(weapon != null ? toItemDto(weapon) : null)
                .armor(armor != null ? toItemDto(armor) : null)
                .build();

        return UnitProfileDto.builder()
                .id(unit.getId())
                .name(unit.getName())
                .level(unit.getLevel())
                .currentExperience(unit.getExperience())
                .experienceForNextLevel(1000L * unit.getLevel())
                .equipment(equipmentDto)
                .totalStats(totalStats)
                .build();
    }

    private UnitProfileDto.ItemDto toItemDto(Item item) {
        if (item == null) return null;
        return UnitProfileDto.ItemDto.builder()
                .itemKey(item.getItemKey())
                .name(item.getName())
                .imageUrl(item.getImageUrl())
                .build();
    }
}