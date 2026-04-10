package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.*;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.exception.InvalidMoveException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleLocationService {

    private final RedisService redisService;
    private final BattleConfigService battleConfigService;

    private static final String BATTLE_STATE_KEY_PREFIX = "battle:state:";
    private static final Duration BATTLE_STATE_TTL = Duration.ofHours(1);

    private String getKeyForUser(Long userId) {
        return BATTLE_STATE_KEY_PREFIX + userId;
    }

    public BattleLocationDto getOrCreateBattleLocation(Long userId, String locationId) {
        String userKey = getKeyForUser(userId);

        BattleLocationDto currentState = redisService.get(userKey, BattleLocationDto.class);
        if (currentState != null) {
            return currentState;
        }

        LocationConfig config = battleConfigService.getLocationConfig(locationId);

        Set<CoordinateDto> blockedSet = new HashSet<>(config.getBlockedCells());
        List<CoordinateDto> initialAllyPlacementZone = generatePlacementZone(config.getAllyPlacement(), config.getGridWidth(), blockedSet);
        List<CoordinateDto> allyPlacementCells = new ArrayList<>(initialAllyPlacementZone);
        List<CoordinateDto> enemyPlacementCells = generatePlacementZone(config.getEnemyPlacement(), config.getGridWidth(), blockedSet);

        List<BattlePirateDto> pirates = new ArrayList<>();
        Collections.shuffle(allyPlacementCells);
        Collections.shuffle(enemyPlacementCells);

        for (LocationConfig.SquadMember member : config.getSquad()) {
            LocationConfig.UnitConfig unitConfig = config.getUnits().get(member.getUnitType());
            List<CoordinateDto> placementZone = (member.getTeam() == TeamType.ALLY) ? allyPlacementCells : enemyPlacementCells;

            for (int i = 0; i < member.getCount(); i++) {
                if (!placementZone.isEmpty()) {
                    CoordinateDto position = placementZone.removeFirst();
                    pirates.add(createPirateFromConfig(unitConfig, member.getTeam(), position));
                }
            }
        }

        BattleLocationDto newLocation = new BattleLocationDto(
                config.getLocationImageId(),
                pirates,
                config.getGridWidth(),
                config.getGridHeight(),
                config.getBlockedCells(),
                allyPlacementCells,
                enemyPlacementCells,
                initialAllyPlacementZone
        );

        redisService.set(userKey, newLocation, BATTLE_STATE_TTL);
        return newLocation;
    }

    public List<BattlePirateDto> movePirateDuringPlacement(Long userId, PirateMoveRequestDto request) {
        log.info("=== MOVE PIRATE DEBUG ===");
        log.info("Target coordinates: q={}, r={}", request.getTargetQ(), request.getTargetR());

        String userKey = getKeyForUser(userId);
        BattleLocationDto currentLocation = redisService.get(userKey, BattleLocationDto.class);

        if (currentLocation == null) {
            throw new IllegalStateException("Состояние боя не найдено или истекло. Начните новый бой.");
        }

        log.info("Ally placement zone size: {}", currentLocation.getAllyInitialPlacementZone().size());
        log.info("Ally placement zone: {}", currentLocation.getAllyInitialPlacementZone());
        log.info("Blocked cells size: {}", currentLocation.getBlockedCells().size());

        BattlePirateDto draggedPirate = currentLocation.getPirates().stream()
                .filter(p -> p.getId().equals(request.getPirateId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Перемещаемый пират с ID " + request.getPirateId() + " не найден."));

        log.info("Dragged pirate current position: q={}, r={}", draggedPirate.getQ(), draggedPirate.getR());

        if (draggedPirate.getTeam() != TeamType.ALLY) {
            throw new InvalidMoveException("Нельзя перемещать пиратов противника.");
        }

        boolean isInPlacementZone = currentLocation.getAllyInitialPlacementZone().stream()
                .anyMatch(cell -> cell.getQ() == request.getTargetQ() && cell.getR() == request.getTargetR());

        log.info("Is in placement zone: {}", isInPlacementZone);

        if (!isInPlacementZone) {
            throw new InvalidMoveException("Недопустимая позиция для расстановки.");
        }

        boolean isBlocked = currentLocation.getBlockedCells().stream()
                .anyMatch(cell -> cell.getQ() == request.getTargetQ() && cell.getR() == request.getTargetR());

        log.info("Is blocked: {}", isBlocked);

        if (isBlocked) {
            throw new InvalidMoveException("Нельзя разместиться в заблокированной ячейке.");
        }

        Optional<BattlePirateDto> occupyingPirateOpt = currentLocation.getPirates().stream()
                .filter(p -> p.getQ() == request.getTargetQ()
                        && p.getR() == request.getTargetR()
                        && !p.getId().equals(draggedPirate.getId()))
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

        redisService.set(userKey, currentLocation, BATTLE_STATE_TTL);
        return currentLocation.getPirates();
    }

    /**
     * Удаляет состояние боя из Redis после его завершения.
     */
    public void endBattle(Long userId) {
        redisService.delete(getKeyForUser(userId));
    }

    private BattlePirateDto createPirateFromConfig(LocationConfig.UnitConfig config, TeamType team, CoordinateDto position) {
        return new BattlePirateDto(
                UUID.randomUUID().toString(),
                team,
                config.getHp(),
                config.getMinAttack(),
                config.getMaxAttack(),
                config.getArmor(),
                config.getXp(),
                position.getQ(),
                position.getR(),
                config.getImageId(),
                config.getMovement(),
                config.getAttackSpeed(),
                config.getRange()
        );
    }

    private List<CoordinateDto> generatePlacementZone(LocationConfig.PlacementZone zone, int gridWidth, Set<CoordinateDto> blockedSet) {
        List<CoordinateDto> result = IntStream.range(zone.getFromR(), zone.getToR())
                .boxed()
                .flatMap(r -> IntStream.range(0, gridWidth)
                        .mapToObj(q -> new CoordinateDto(q, r)))
                .filter(cell -> !blockedSet.contains(cell))
                .collect(Collectors.toList());

        log.info("Generated placement zone: fromR={}, toR={}, size={}",
                zone.getFromR(), zone.getToR(), result.size());

        return result;
    }
}