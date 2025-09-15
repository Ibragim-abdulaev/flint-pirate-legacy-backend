package org.example.piratelegacy.auth.dto;

import org.example.piratelegacy.auth.dto.BattlePirateDto;

import java.util.List;

public class BattleLogResultDto {
    private String winnerTeam; // "ALLY" или "ENEMY"
    private List<BattleLogEntryDto> log;
    private List<BattlePirateDto> survivingPirates; // выжившие после боя

    public BattleLogResultDto(String winnerTeam, List<BattleLogEntryDto> log, List<BattlePirateDto> survivingPirates) {
        this.winnerTeam = winnerTeam;
        this.log = log;
        this.survivingPirates = survivingPirates;
    }

    public String getWinnerTeam() { return winnerTeam; }
    public List<BattleLogEntryDto> getLog() { return log; }
    public List<BattlePirateDto> getSurvivingPirates() { return survivingPirates; }
}