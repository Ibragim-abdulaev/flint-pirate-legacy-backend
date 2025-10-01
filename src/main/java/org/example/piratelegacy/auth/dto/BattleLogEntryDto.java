package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattleLogEntryDto implements Serializable {
    private LogEntryType type; // Тип события: MOVE, ATTACK, DEATH
    private String unitId;     // ID юнита, который совершил действие
    private Map<String, Object> details; // Детали события

    public enum LogEntryType {
        TURN_START,
        MOVE,
        ATTACK,
        DEATH
    }
}