package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * Находит все предметы в инвентаре пользователя.
     * @param userId ID пользователя
     * @return Список предметов в инвентаре
     */
    List<InventoryItem> findByUserId(Long userId);

    /**
     * Находит конкретный стак предметов в инвентаре пользователя по ID самого предмета.
     * @param userId ID пользователя
     * @param itemId ID предмета (из таблицы items)
     * @return Optional с предметом инвентаря, если найден
     */
    Optional<InventoryItem> findByUserIdAndItemId(Long userId, Long itemId);
}