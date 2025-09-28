package org.example.piratelegacy.auth.service;

import org.example.piratelegacy.auth.dto.BattleLocationDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.dto.CoordinateDto;
import org.example.piratelegacy.auth.dto.PirateMoveRequestDto;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.exception.InvalidMoveException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleLocationService {

    private final Map<Long, BattleLocationDto> battleStates = new ConcurrentHashMap<>();
    private static final int GRID_WIDTH = 15;
    private static final int GRID_HEIGHT = 20;
    private static final int ALLY_COUNT = 6;
    private static final int ENEMY_COUNT = 4;
    private static final int BASE_HP = 100;
    private static final int MIN_ATTACK = 10;
    private static final int MAX_ATTACK = 20;
    private static final int ARMOR = 1;
    private static final int BASE_MOVEMENT = 3;
    private static final int ALLY_XP = 200;
    private static final int ENEMY_XP = 120;

    public BattleLocationDto getFirstQuestLocation(Long userId) {
        if (battleStates.containsKey(userId)) {
            return battleStates.get(userId);
        }

        List<CoordinateDto> blockedCells = getBlockedCells();
        Set<CoordinateDto> blockedSet = new HashSet<>(blockedCells);

        List<CoordinateDto> allyPlacementCells = generatePlacementZone(GRID_HEIGHT - 3, GRID_HEIGHT, blockedSet);
        List<CoordinateDto> enemyPlacementCells = generatePlacementZone(0, 3, blockedSet);

        List<BattlePirateDto> pirates = new ArrayList<>();

        Collections.shuffle(allyPlacementCells);
        Collections.shuffle(enemyPlacementCells);

        for (int i = 0; i < ALLY_COUNT; i++) {
            CoordinateDto position = allyPlacementCells.get(i);
            pirates.add(generatePirate(TeamType.ALLY, ALLY_XP, "ally_pirate_01", position.getQ(), position.getR()));
        }

        for (int i = 0; i < ENEMY_COUNT; i++) {
            CoordinateDto position = enemyPlacementCells.get(i);
            pirates.add(generatePirate(TeamType.ENEMY, ENEMY_XP, "enemy_pirate_01", position.getQ(), position.getR()));
        }

        BattleLocationDto location = new BattleLocationDto(
                "battle_location_01",
                pirates,
                GRID_WIDTH,
                GRID_HEIGHT,
                blockedCells,
                allyPlacementCells,
                enemyPlacementCells
        );

        battleStates.put(userId, location);
        return location;
    }

    public List<BattlePirateDto> movePirateDuringPlacement(Long userId, PirateMoveRequestDto request) {
        BattleLocationDto currentLocation = battleStates.get(userId);
        if (currentLocation == null) {
            throw new IllegalStateException("Состояние боя не найдено. Начните новый бой.");
        }

        BattlePirateDto draggedPirate = currentLocation.getPirates().stream()
                .filter(p -> p.getId().equals(request.getPirateId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Перемещаемый пират с ID " + request.getPirateId() + " не найден."));

        if (draggedPirate.getTeam() != TeamType.ALLY) {
            throw new InvalidMoveException("Нельзя перемещать пиратов противника.");
        }

        boolean isTargetCellValid = currentLocation.getAllyPlacementCells().stream()
                .anyMatch(cell -> cell.getQ() == request.getTargetQ() && cell.getR() == request.getTargetR());

        if (!isTargetCellValid) {
            throw new InvalidMoveException("Недопустимая позиция для расстановки.");
        }

        Optional<BattlePirateDto> occupyingPirateOpt = currentLocation.getPirates().stream()
                .filter(p -> p.getQ() == request.getTargetQ() && p.getR() == request.getTargetR() && !p.getId().equals(draggedPirate.getId()))
                .findFirst();

        if (occupyingPirateOpt.isPresent()) {
            BattlePirateDto occupyingPirate = occupyingPirateOpt.get();
            if (occupyingPirate.getTeam() != TeamType.ALLY) {
                throw new InvalidMoveException("Нельзя разместиться в ячейке, занятой противником.");
            }

            int oldQ = draggedPirate.getQ();
            int oldR = draggedPirate.getR();

            draggedPirate.setQ(request.getTargetQ());
            draggedPirate.setR(request.getTargetR());

            occupyingPirate.setQ(oldQ);
            occupyingPirate.setR(oldR);
        } else {
            draggedPirate.setQ(request.getTargetQ());
            draggedPirate.setR(request.getTargetR());
        }

        return currentLocation.getPirates();
    }


    private BattlePirateDto generatePirate(TeamType team, int xp, String imageId, int q, int r) {
        return new BattlePirateDto(
                UUID.randomUUID().toString(), team, BASE_HP, MIN_ATTACK, MAX_ATTACK,
                ARMOR, xp, q, r, imageId, BASE_MOVEMENT);
    }

    private List<CoordinateDto> getBlockedCells() {
        return Arrays.asList(
                new CoordinateDto(8, 0), new CoordinateDto(8, 1), new CoordinateDto(9, 0), new CoordinateDto(9, 1),
                new CoordinateDto(9, 2), new CoordinateDto(9, 3), new CoordinateDto(9, 4), new CoordinateDto(9, 5),
                new CoordinateDto(9, 6), new CoordinateDto(8, 4), new CoordinateDto(8, 5), new CoordinateDto(8, 6),
                new CoordinateDto(0, 0), new CoordinateDto(0, 1), new CoordinateDto(0, 2), new CoordinateDto(0, 3),
                new CoordinateDto(0, 4), new CoordinateDto(1, 0), new CoordinateDto(1, 1), new CoordinateDto(1, 2),
                new CoordinateDto(1, 3), new CoordinateDto(1, 4)
        );
    }

    private List<CoordinateDto> generatePlacementZone(int fromR, int toR, Set<CoordinateDto> blockedSet) {
        List<CoordinateDto> placementCells = new ArrayList<>();
        for (int r = fromR; r < toR; r++) {
            for (int q = 0; q < GRID_WIDTH; q++) {
                CoordinateDto cell = new CoordinateDto(q, r);
                if (!blockedSet.contains(cell)) {
                    placementCells.add(cell);
                }
            }
        }
        return placementCells;
    }
}