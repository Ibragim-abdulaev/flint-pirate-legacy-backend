package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRewardDto implements Serializable {
    private String itemKey;       // Уникальный ключ предмета (например, "ice_shard_drake")
    private String name;          // Название для отображения ("Осколок льда Дрейка")
    private String imageUrl;      // URL иконки предмета
    private int quantity;         // Количество
}