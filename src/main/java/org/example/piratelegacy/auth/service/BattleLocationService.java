package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.*;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.UserShip;
import org.example.piratelegacy.auth.entity.enums.ShipMode;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.exception.InvalidMoveException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.example.piratelegacy.auth.repository.UserShipRepository;
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
    private final UserShipRepository shipRepository;

    private static final String BATTLE_STATE_KEY_PREFIX = "battle:state:";
    private static final Duration BATTLE_STATE_TTL = Duration.ofHours(1);

    private String getKeyForUser(Long userId) {
        return BATTLE_STATE_KEY_PREFIX + userId;
    }

    public BattleLocationDto getOrCreateBattleLocation(Long userId, String locationId) {
        String userKey = getKeyForUser(userId);

        BattleLocationDto currentState = redisService.get(userKey, BattleLocationDto.class);
        if (currentState != null) return currentState;

        LocationConfig config = battleConfigService.getLocationConfig(locationId);
        Set<CoordinateDto> blockedSet = new HashSet<>(config.getBlockedCells());

        List<CoordinateDto> initialAllyZone = generatePlacementZone(config.getAllyPlacement(), config.getGridWidth(), blockedSet);
        List<CoordinateDto> allyPlacementCells = new ArrayList<>(initialAllyZone);
        List<CoordinateDto> enemyPlacementCells = generatePlacementZone(config.getEnemyPlacement(), config.getGridWidth(), blockedSet);

        Collections.shuffle(allyPlacementCells);
        Collections.shuffle(enemyPlacementCells);

        List<BattlePirateDto> pirates = new ArrayList<>();

        boolean hasAllyInConfig = config.getSquad() != null &&
                config.getSquad().stream().anyMatch(m -> m.getTeam() == TeamType.ALLY);

        if (hasAllyInConfig) {
            // Показательный бой — союзники из конфига
            log.info("Battle '{}' — showcase, using config allies", locationId);
            for (LocationConfig.SquadMember member : config.getSquad()) {
                if (member.getTeam() != TeamType.ALLY) continue;
                LocationConfig.UnitConfig unitConfig = config.getUnits().get(member.getUnitType());
                if (unitConfig == null) continue;
                for (int i = 0; i < member.getCount(); i++) {
                    if (allyPlacementCells.isEmpty()) break;
                    pirates.add(createPirateFromConfig(unitConfig, TeamType.ALLY, allyPlacementCells.removeFirst()));
                }
            }
        } else {
            // Обычный бой — берём юнитов с корабля в режиме ATTACK
            List<Unit> crew = getAttackShipCrew(userId);
            if (crew.isEmpty()) {
                throw new ApiException(
                        "Нет юнитов для боя. Посадите пиратов на корабль и поставьте его режим 'В атаку'.",
                        HttpStatus.BAD_REQUEST);
            }
            log.info("Battle '{}' — using {} units from ATTACK ship", locationId, crew.size());
            for (Unit unit : crew) {
                if (allyPlacementCells.isEmpty()) break;
                pirates.add(createPirateFromUnit(unit, allyPlacementCells.removeFirst()));
            }
        }

        // Враги всегда из конфига
        for (LocationConfig.SquadMember member : config.getSquad()) {
            if (member.getTeam() != TeamType.ENEMY) continue;
            LocationConfig.UnitConfig unitConfig = config.getUnits().get(member.getUnitType());
            if (unitConfig == null) continue;
            for (int i = 0; i < member.getCount(); i++) {
                if (enemyPlacementCells.isEmpty()) break;
                pirates.add(createPirateFromConfig(unitConfig, TeamType.ENEMY, enemyPlacementCells.removeFirst()));
            }
        }

        BattleLocationDto newLocation = new BattleLocationDto(
                config.getLocationImageId(), pirates,
                config.getGridWidth(), config.getGridHeight(),
                config.getBlockedCells(), allyPlacementCells,
                enemyPlacementCells, initialAllyZone);

        redisService.set(userKey, newLocation, BATTLE_STATE_TTL);
        return newLocation;
    }

    private List<Unit> getAttackShipCrew(Long userId) {
        Optional<UserShip> attackShip = shipRepository.findByOwnerIdAndMode(userId, ShipMode.ATTACK);
        if (attackShip.isEmpty()) return Collections.emptyList();
        return unitRepository.findByShipId(attackShip.get().getId()).stream()
                .filter(Unit::isAlive)
                .collect(Collectors.toList());
    }

    public List<BattlePirateDto> movePirateDuringPlacement(Long userId, PirateMoveRequestDto request) {
        String userKey = getKeyForUser(userId);
        BattleLocationDto currentLocation = redisService.get(userKey, BattleLocationDto.class);
        if (currentLocation == null) {
            throw new IllegalStateException("Состояние боя не найдено. Начните новый бой.");
        }

        BattlePirateDto dragged = currentLocation.getPirates().stream()
                .filter(p -> p.getId().equals(request.getPirateId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Пират " + request.getPirateId() + " не найден."));

        if (dragged.getTeam() != TeamType.ALLY) {
            throw new InvalidMoveException("Нельзя перемещать пиратов противника.");
        }

        boolean inZone = currentLocation.getAllyInitialPlacementZone().stream()
                .anyMatch(c -> c.getQ() == request.getTargetQ() && c.getR() == request.getTargetR());
        if (!inZone) throw new InvalidMoveException("Недопустимая позиция.");

        boolean blocked = currentLocation.getBlockedCells().stream()
                .anyMatch(c -> c.getQ() == request.getTargetQ() && c.getR() == request.getTargetR());
        if (blocked) throw new InvalidMoveException("Ячейка заблокирована.");

        Optional<BattlePirateDto> occupying = currentLocation.getPirates().stream()
                .filter(p -> p.getQ() == request.getTargetQ() && p.getR() == request.getTargetR()
                        && !p.getId().equals(dragged.getId()))
                .findFirst();

        if (occupying.isPresent()) {
            if (occupying.get().getTeam() != TeamType.ALLY)
                throw new InvalidMoveException("Ячейка занята противником.");
            int oldQ = dragged.getQ(), oldR = dragged.getR();
            dragged.setQ(request.getTargetQ()); dragged.setR(request.getTargetR());
            occupying.get().setQ(oldQ); occupying.get().setR(oldR);
        } else {
            dragged.setQ(request.getTargetQ());
            dragged.setR(request.getTargetR());
        }

        redisService.set(userKey, currentLocation, BATTLE_STATE_TTL);
        return currentLocation.getPirates();
    }

    public void endBattle(Long userId) {
        redisService.delete(getKeyForUser(userId));
    }

    private BattlePirateDto createPirateFromUnit(Unit unit, CoordinateDto pos) {
        return new BattlePirateDto(
                String.valueOf(unit.getId()), TeamType.ALLY,
                unit.getBaseHp(), unit.getBaseMinAttack(), unit.getBaseMaxAttack(), unit.getBaseArmor(),
                0, pos.getQ(), pos.getR(), unit.getUnitTypeKey(), 3, 1, 1);
    }

    private BattlePirateDto createPirateFromConfig(LocationConfig.UnitConfig cfg, TeamType team, CoordinateDto pos) {
        return new BattlePirateDto(
                UUID.randomUUID().toString(), team,
                cfg.getHp(), cfg.getMinAttack(), cfg.getMaxAttack(), cfg.getArmor(),
                cfg.getXp(), pos.getQ(), pos.getR(), cfg.getImageId(),
                cfg.getMovement(), cfg.getAttackSpeed(), cfg.getRange());
    }

    private List<CoordinateDto> generatePlacementZone(LocationConfig.PlacementZone zone, int gridWidth, Set<CoordinateDto> blocked) {
        return IntStream.range(zone.getFromR(), zone.getToR()).boxed()
                .flatMap(r -> IntStream.range(0, gridWidth).mapToObj(q -> new CoordinateDto(q, r)))
                .filter(c -> !blocked.contains(c))
                .collect(Collectors.toList());
    }
}