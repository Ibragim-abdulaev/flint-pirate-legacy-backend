package org.example.piratelegacy.auth.dto;

public class BattleLogEntryDto {
    private String attackerId;
    private String targetId;
    private int damage;
    private boolean targetDead;

    public BattleLogEntryDto(String attackerId, String targetId, int damage, boolean targetDead) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.damage = damage;
        this.targetDead = targetDead;
    }

    public String getAttackerId() { return attackerId; }
    public String getTargetId() { return targetId; }
    public int getDamage() { return damage; }
    public boolean isTargetDead() { return targetDead; }
}