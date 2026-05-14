package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TavernUnitDto implements Serializable {
    private String unitTypeKey;
    private String name;
    private String combatClass;
    private int baseHp;
    private int baseMinAttack;
    private int baseMaxAttack;
    private int baseArmor;
    private long goldCost;
    private long crystalsCost;
}