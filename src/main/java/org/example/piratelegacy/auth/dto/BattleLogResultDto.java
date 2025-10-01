package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.TeamType;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattleLogResultDto implements Serializable {
    private TeamType winnerTeam;
    private List<BattleLogEntryDto> log;
    private List<BattlePirateDto> survivingPirates;
}