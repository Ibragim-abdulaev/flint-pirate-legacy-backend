package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.*;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.exception.InvalidMoveException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.http.HttpStatus;
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
    private final UnitRepository unitRepository;

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

        Collections.shuffle(allyPlacementCells);
        Collections.shuffle(enemyPlacementCells);

        List<BattlePirateDto> pirates = new ArrayList<>();

        // Проверяем есть ли союзники в конфиге локации
        boolean hasAllyInConfig = config.getSquad() != null && config.getSquad().stream()
                .anyMatch(m -> m.getTeam() == TeamType.ALLY);

        if (hasAllyInConfig) {
            // Показательный бой — союзники из конфига (UUID, опыт не начисляется)
            log.info("Battle location '{}' uses config-based allies (showcase battle)", locationId);
            for (LocationConfig.SquadMember member : config.getSquad()) {
                if (member.getTeam() != TeamType.ALLY) continue;
                LocationConfig.UnitConfig unitConfig = config.getUnits().get(member.getUnitType());
                if (unitConfig == null) continue;
                for (int i = 0; i < member.getCount(); i++) {
                    if (allyPlacementCells.isEmpty()) break;
                    CoordinateDto position = allyPlacementCells.removeFirst();
                    pirates.add(createPirateFromConfig(unitConfig, TeamType.ALLY, position));
                }
            }
        } else {
            // Обычный бой — союзники из базы игрока с реальными ID
            log.info("Battle location '{}' uses player's units", locationId);
            List<Unit> aliveUnits = unitRepository.findByOwnerId(userId).stream()
                    .filter(Unit::isAlive)
                    .collect(Collectors.toList());

            if (aliveUnits.isEmpty()) {
                throw new ApiException("У вас нет живых юнитов для участия в бою.", HttpStatus.BAD_REQUEST);
            }

            for (Unit unit : aliveUnits) {
                if (allyPlacementCells.isEmpty()) break;
                CoordinateDto position = allyPlacementCells.removeFirst();
                pirates.add(createPirateFromUnit(unit, position));
            }
        }

        // Враги — всегда из конфига
        for (LocationConfig.SquadMember member : config.getSquad()) {
            if (member.getTeam() != TeamType.ENEMY) continue;
            LocationConfig.UnitConfig unitConfig = config.getUnits().get(member.getUnitType());
            if (unitConfig == null) continue;
            for (int i = 0; i < member.getCount(); i++) {
                if (enemyPlacementCells.isEmpty()) break;
                CoordinateDto position = enemyPlacementCells.removeFirst();
                pirates.add(createPirateFromConfig(unitConfig, TeamType.ENEMY, position));
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
        String userKey = getKeyForUser(userId);
        BattleLocationDto currentLocation = redisService.get(userKey, BattleLocationDto.class);

        if (currentLocation == null) {
            throw new IllegalStateException("Состояние боя не найдено или истекло. Начните новый бой.");
        }

        BattlePirateDto draggedPirate = currentLocation.getPirates().stream()
                .filter(p -> p.getId().equals(request.getPirateId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Перемещаемый пират с ID " + request.getPirateId() + " не найден."));

        if (draggedPirate.getTeam() != TeamType.ALLY) {
            throw new InvalidMoveException("Нельзя перемещать пиратов противника.");
        }

        boolean isInPlacementZone = currentLocation.getAllyInitialPlacementZone().stream()
                .anyMatch(cell -> cell.getQ() == request.getTargetQ() && cell.getR() == request.getTargetR());

        if (!isInPlacementZone) {
            throw new InvalidMoveException("Недопустимая позиция для расстановки.");
        }

        boolean isBlocked = currentLocation.getBlockedCells().stream()
                .anyMatch(cell -> cell.getQ() == request.getTargetQ() && cell.getR() == request.getTargetR());

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

    public void endBattle(Long userId) {
        redisService.delete(getKeyForUser(userId));
    }

    // Союзный юнит из базы — реальный ID, опыт начисляется после боя
    private BattlePirateDto createPirateFromUnit(Unit unit, CoordinateDto position) {
        return new BattlePirateDto(
                String.valueOf(unit.getId()),
                TeamType.ALLY,
                unit.getBaseHp(),
                unit.getBaseMinAttack(),
                unit.getBaseMaxAttack(),
                unit.getBaseArmor(),
                0,
                position.getQ(),
                position.getR(),
                unit.getUnitTypeKey(),
                3, // movement — одинаковый для всех пока
                1, // attackSpeed
                1  // range
        );
    }

    // Юнит из конфига — UUID, опыт не начисляется
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