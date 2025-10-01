package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.service.GameConfigService;

import java.io.Serializable;

@Data
@AllArgsConstructor // <-- УБЕДИТЕСЬ, ЧТО ЭТА АННОТАЦИЯ ЕСТЬ
@NoArgsConstructor
public class CharacterOptionDto implements Serializable {
    private String characterType;
    private String displayName;
    private String combatClass;
    private int baseHp;
    private int minAttack;
    private int maxAttack;
    private int baseArmor;
    private String specialAbility;
    private boolean hasSpecialAbility;

    public static CharacterOptionDto fromCharacterStats(GameConfigService.CharacterStats stats) {
        return new CharacterOptionDto(
                stats.getCharacterType().name(),
                stats.getDisplayName(),
                stats.getCombatClass().name(),
                stats.getBaseHp(),
                stats.getMinAttack(),
                stats.getMaxAttack(),
                stats.getBaseArmor(),
                stats.getSpecialAbility(),
                stats.getSpecialAbility() != null && !stats.getSpecialAbility().isBlank()
        );
    }
}