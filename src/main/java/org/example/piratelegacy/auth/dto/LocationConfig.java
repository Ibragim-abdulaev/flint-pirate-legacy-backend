package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.TeamType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationConfig implements Serializable {
    private String locationImageId;
    private int gridWidth;
    private int gridHeight;
    private List<CoordinateDto> blockedCells;
    private PlacementZone allyPlacement;
    private PlacementZone enemyPlacement;
    private Map<String, UnitConfig> units;
    private List<SquadMember> squad;

    @Data
    public static class PlacementZone implements Serializable {
        private int fromR;
        private int toR;
    }

    @Data
    public static class UnitConfig implements Serializable {
        private int hp;
        private int minAttack;
        private int maxAttack;
        private int armor;
        private int movement;
        private int attackSpeed;
        private int range;
        private int xp;
        private String imageId;
    }

    @Data
    public static class SquadMember implements Serializable {
        private String unitType;
        private TeamType team;
        private int count;
    }
}