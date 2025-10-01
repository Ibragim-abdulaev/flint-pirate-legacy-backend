package org.example.piratelegacy.auth.entity;

import lombok.Getter;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.entity.enums.TeamType;

@Getter
public class BattlePirate {
    private final String id;
    private final TeamType team;
    private int hp;
    private final int minAttack;
    private final int maxAttack;
    private final int armor;

    public BattlePirate(BattlePirateDto dto) {
        this.id = dto.getId();
        this.team = dto.getTeam();
        this.hp = dto.getHp();
        this.minAttack = dto.getMinAttack();
        this.maxAttack = dto.getMaxAttack();
        this.armor = dto.getArmor();
    }

    public void applyDamage(int dmg) {
        this.hp -= dmg;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}