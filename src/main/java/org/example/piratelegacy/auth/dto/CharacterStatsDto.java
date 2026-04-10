package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.CharacterType;
import org.example.piratelegacy.auth.entity.enums.CombatClass;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterStatsDto implements Serializable {
    private CharacterType characterType;
    private String displayName;
    private CombatClass combatClass;
    private int baseHp;
    private int minAttack;
    private int maxAttack;
    private int baseArmor;
    private String specialAbility;
}