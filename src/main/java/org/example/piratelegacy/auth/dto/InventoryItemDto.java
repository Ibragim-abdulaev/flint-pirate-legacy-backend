package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor // <-- УБЕДИТЕСЬ, ЧТО ЭТА АННОТАЦИЯ ЕСТЬ
@NoArgsConstructor
@Builder
public class InventoryItemDto implements Serializable {
    private Long inventoryId; // Уникальный ID записи в инвентаре (не предмета!)
    private String itemKey;
    private String name;
    private String imageUrl;
    private int quantity;
    // Можно добавить и другие поля из Item, если они нужны в инвентаре
}