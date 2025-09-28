package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.TeamType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattlePirateDto {
    private String id;
    private TeamType team;
    private int hp;
    private int minAttack;
    private int maxAttack;
    private int armor;
    private int xp;
    private int q;
    private int r;
    private String imageId;
    private int movement;
}