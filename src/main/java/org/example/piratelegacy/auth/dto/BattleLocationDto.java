package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // Добавим NoArgsConstructor для удобства

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor // Добавим
public class BattleLocationDto {
    private String locationImageId;
    private List<BattlePirateDto> pirates;

    // Новые поля для конфигурации сетки
    private int gridWidth;
    private int gridHeight;
    private List<CoordinateDto> blockedCells;
    private List<CoordinateDto> allyPlacementCells;
    private List<CoordinateDto> enemyPlacementCells;
}