package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BattleLogEntry {
    private String attackerTeam;
    private Long attackerId;
    private String targetTeam;
    private Long targetId;
    private int damage;
    private int targetHpAfter;
}