package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.InventoryItem;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.ItemType;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.InventoryItemRepository;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final UnitRepository unitRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Экипирует предмет из инвентаря на указанного юнита.
     * @param user Владелец
     * @param unitId ID юнита, на которого надевается предмет
     * @param inventoryItemId ID предмета в инвентаре
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "units", key = "#unitId"),
            @CacheEvict(cacheNames = "units", key = "'team_summary:' + #user.id")
    })
    public void equipItem(User user, Long unitId, Long inventoryItemId) {
        Unit unit = unitRepository.findByIdAndOwnerId(unitId, user.getId())
                .orElseThrow(() -> new ApiException("Юнит не найден или не принадлежит вам.", HttpStatus.NOT_FOUND));

        InventoryItem itemToEquip = inventoryItemRepository.findById(inventoryItemId)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiException("Предмет в инвентаре не найден.", HttpStatus.NOT_FOUND));

        if (itemToEquip.getItem().getItemType() == ItemType.WEAPON) {
            unit.setEquippedWeapon(itemToEquip);
        } else if (itemToEquip.getItem().getItemType() == ItemType.ARMOR) {
            unit.setEquippedArmor(itemToEquip);
        } else {
            throw new ApiException("Предмет неизвестного типа.", HttpStatus.BAD_REQUEST);
        }

        unitRepository.save(unit);
    }

    /**
     * Снимает предмет с указанного юнита из указанного слота.
     * @param user Владелец
     * @param unitId ID юнита
     * @param itemType Тип слота (WEAPON или ARMOR)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "units", key = "#unitId"),
            @CacheEvict(cacheNames = "units", key = "'team_summary:' + #user.id")
    })
    public void unequipItem(User user, Long unitId, ItemType itemType) {
        Unit unit = unitRepository.findByIdAndOwnerId(unitId, user.getId())
                .orElseThrow(() -> new ApiException("Юнит не найден или не принадлежит вам.", HttpStatus.NOT_FOUND));

        if (itemType == ItemType.WEAPON) {
            unit.setEquippedWeapon(null);
        } else if (itemType == ItemType.ARMOR) {
            unit.setEquippedArmor(null);
        }

        unitRepository.save(unit);
    }
}