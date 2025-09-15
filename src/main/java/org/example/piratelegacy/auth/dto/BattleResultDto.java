package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BattleResultDto {
    private String winnerTeam; // "ALLY" или "ENEMY"
    private List<BattlePirateDto> survivingPirates;
}
