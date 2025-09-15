package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CharacterInfo {
    private String characterType;
    private String displayName;
    private String combatClass;
    private String combatClassRus;
    private int baseHp;
    private int minAttack;
    private int maxAttack;
    private int baseArmor;
    private String specialAbility;
    private boolean hasSpecialAbility;
}
