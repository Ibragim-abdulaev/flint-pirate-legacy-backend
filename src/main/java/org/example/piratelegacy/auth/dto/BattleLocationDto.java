package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattleLocationDto implements Serializable {
    private String locationImageId;
    private List<BattlePirateDto> pirates;
    private int gridWidth;
    private int gridHeight;
    private List<CoordinateDto> blockedCells;
    private List<CoordinateDto> allyPlacementCells;
    private List<CoordinateDto> enemyPlacementCells;
    private List<CoordinateDto> allyInitialPlacementZone;
}