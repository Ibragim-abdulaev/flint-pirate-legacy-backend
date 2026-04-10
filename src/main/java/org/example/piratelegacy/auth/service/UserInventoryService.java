package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.InventoryItemDto;
import org.example.piratelegacy.auth.entity.InventoryItem;
import org.example.piratelegacy.auth.entity.QuestItemReward;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Возвращает все предметы в инвентаре пользователя.
     */
    @Transactional(readOnly = true)
    public List<InventoryItemDto> getInventory(User user) {
        return inventoryItemRepository.findByUserId(user.getId()).stream()
                .map(invItem -> InventoryItemDto.builder()
                        .inventoryId(invItem.getId())
                        .itemKey(invItem.getItem().getItemKey())
                        .name(invItem.getItem().getName())
                        .imageUrl(invItem.getItem().getImageUrl())
                        .quantity(invItem.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Добавляет предметы в инвентарь пользователя.
     * Если предмет уже есть, увеличивает его количество (стакает).
     * @param user Пользователь, получающий предметы.
     * @param rewards Список предметов-наград из квеста.
     */
    @Transactional
    public void addItemsToInventory(User user, List<QuestItemReward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        for (QuestItemReward reward : rewards) {
            inventoryItemRepository.findByUserIdAndItemId(user.getId(), reward.getItem().getId())
                    .ifPresentOrElse(

                            existingItem -> {
                                existingItem.setQuantity(existingItem.getQuantity() + reward.getQuantity());
                                inventoryItemRepository.save(existingItem);
                                log.info("Пользователю ID {} добавлено {}x '{}' (итого: {})", user.getId(), reward.getQuantity(), reward.getItem().getName(), existingItem.getQuantity());
                            },

                            () -> {
                                InventoryItem newItem = InventoryItem.builder()
                                        .user(user)
                                        .item(reward.getItem())
                                        .quantity(reward.getQuantity())
                                        .build();
                                inventoryItemRepository.save(newItem);
                                log.info("Пользователь ID {} получил новый предмет: {}x '{}'", user.getId(), reward.getQuantity(), reward.getItem().getName());
                            }
                    );
        }
    }
}