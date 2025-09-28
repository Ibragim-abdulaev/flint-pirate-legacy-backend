package org.example.piratelegacy.auth.entity.enums;

import lombok.Getter;
import org.example.piratelegacy.auth.entity.enums.CombatClass;

@Getter
public enum CharacterType {
    BARBARIAN("Варвар", CombatClass.MELEE, 300, 20, 30, 6, null),
    ARCHER("Лучница", CombatClass.RANGED, 250, 35, 45, 4, null),
    VALKYRIE("Валькирия", CombatClass.MELEE, 500, 70, 90, 10, "Критический удар – наносит в два раза больше урона по врагу от максимального урона"),
    GUNNER("Канонир", CombatClass.RANGED, 700, 120, 140, 15, "Шквал огня – поражает всех врагов в радиусе атаки максимально возможным уроном"),
    GLADIATOR("Гладиатор", CombatClass.MELEE, 1000, 100, 120, 20, "Оглушающий удар – оглушает врага физическим ударом на 3 секунды"),
    GRENADIER("Гренадер", CombatClass.RANGED, 900, 140, 180, 15, "Минирование – под врагом взрывается бочка с порохом, нанося врагу в 3 раза больше урона от максимального"),
    SHAMAN("Шаман", CombatClass.RANGED, 800, 180, 200, 10, "Сфера льда – замораживает врага, лишая способности двигаться 5 секунд");

    private final String displayName;
    private final CombatClass combatClass;
    private final int baseHp;
    private final int minAttack;
    private final int maxAttack;
    private final int baseArmor;
    private final String specialAbility;

    CharacterType(String displayName, CombatClass combatClass, int baseHp, int minAttack, int maxAttack, int baseArmor, String specialAbility) {
        this.displayName = displayName;
        this.combatClass = combatClass;
        this.baseHp = baseHp;
        this.minAttack = minAttack;
        this.maxAttack = maxAttack;
        this.baseArmor = baseArmor;
        this.specialAbility = specialAbility;
    }

    public boolean hasSpecialAbility() { return specialAbility != null; }
}