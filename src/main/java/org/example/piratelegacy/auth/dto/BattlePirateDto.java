package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattlePirateDto {
    private String id;        // уникальный идентификатор
    private String team;      // "ALLY" или "ENEMY"
    private int hp;
    private int minAttack;
    private int maxAttack;
    private int armor;
    private int xp;
    private int x;            // позиция на локации
    private int y;
    private String imageId;   // идентификатор изображения пирата
}
