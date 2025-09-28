package org.example.piratelegacy.auth.entity.enums;

public enum CombatClass {
    MELEE("Ближний бой"),
    RANGED("Дальний бой");

    private final String displayName;

    CombatClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}