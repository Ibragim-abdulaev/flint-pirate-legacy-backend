package org.example.piratelegacy.auth.entity;

import org.example.piratelegacy.auth.dto.BattlePirateDto;

public class BattlePirate {
    private String id;
    private String team; // "ALLY" или "ENEMY"
    private int hp;
    private int minAttack;
    private int maxAttack;
    private int armor;

    public BattlePirate(BattlePirateDto dto) {
        this.id = dto.getId();
        this.team = dto.getTeam();
        this.hp = dto.getHp();
        this.minAttack = dto.getMinAttack();
        this.maxAttack = dto.getMaxAttack();
        this.armor = dto.getArmor();
    }

    public String getId() { return id; }
    public String getTeam() { return team; }
    public int getHp() { return hp; }
    public int getMinAttack() { return minAttack; }
    public int getMaxAttack() { return maxAttack; }
    public int getArmor() { return armor; }

    public void applyDamage(int dmg) {
        this.hp -= dmg;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
