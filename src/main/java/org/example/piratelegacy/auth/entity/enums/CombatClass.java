package org.example.piratelegacy.auth.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CombatClass {
    MELEE("Ближний бой"),
    RANGED("Дальний бой");

    private final String displayName;
}