package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnitProfileDto implements Serializable {
    private Long id;
    private String name;
    private int level;
    private long currentExperience;
    private long experienceForNextLevel;

    private EquipmentDto equipment;

    private Stat totalStats;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Stat implements Serializable {
        private int hp;
        private int minAttack;
        private int maxAttack;
        private int armor;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EquipmentDto implements Serializable {
        private ItemDto weapon;
        private ItemDto armor;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto implements Serializable {
        private String itemKey;
        private String name;
        private String imageUrl;
    }
}