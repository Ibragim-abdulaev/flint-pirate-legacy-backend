package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.ShipTypeDto;
import org.example.piratelegacy.auth.dto.UnitSummaryDto;
import org.example.piratelegacy.auth.dto.UserShipDto;
import org.example.piratelegacy.auth.dto.request.AssignUnitToShipRequest;
import org.example.piratelegacy.auth.dto.request.BuyShipRequest;
import org.example.piratelegacy.auth.dto.request.SetShipModeRequest;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserShip;
import org.example.piratelegacy.auth.entity.enums.BuildingType;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.entity.enums.ShipMode;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.example.piratelegacy.auth.repository.UserBuildingRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.example.piratelegacy.auth.repository.UserShipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortService {

    // Максимальное количество кораблей зависит от уровня порта
    // Уровень 1 = 1 причал, уровень 2 = 2 причала и т.д.
    private static final int MAX_SHIP_UPGRADES = 2;

    private final UserShipRepository shipRepository;
    private final UnitRepository unitRepository;
    private final UserBuildingRepository buildingRepository;
    private final UserResourcesRepository resourcesRepository;
    private final UserProgressService userProgressService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${game.config.ship-types-path:classpath:game/ship_types.json}")
    private String shipTypesPath;

    private Map<String, ShipTypeDto> shipTypesMap;

    @PostConstruct
    public void init() {
        try (InputStream is = resourceLoader.getResource(shipTypesPath).getInputStream()) {
            List<ShipTypeDto> types = objectMapper.readValue(is, new TypeReference<>() {});
            shipTypesMap = types.stream()
                    .collect(Collectors.toUnmodifiableMap(ShipTypeDto::getShipTypeKey, Function.identity()));
            log.info("Loaded {} ship types", shipTypesMap.size());
        } catch (IOException e) {
            log.error("Failed to load ship types", e);
            shipTypesMap = Collections.emptyMap();
        }
    }

    public List<ShipTypeDto> getAvailableShipTypes() {
        return List.copyOf(shipTypesMap.values());
    }

    public List<UserShipDto> getUserShips(User user) {
        return shipRepository.findByOwnerId(user.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserShipDto buyShip(User user, BuyShipRequest request) {
        // Проверяем что порт есть
        int portLevel = buildingRepository.findByOwnerIdAndBuildingType(user.getId(), BuildingType.PORT)
                .orElseThrow(() -> new ApiException("Порт не найден.", HttpStatus.BAD_REQUEST))
                .getLevel();

        // Количество кораблей не может превышать уровень порта
        int shipCount = shipRepository.countByOwnerId(user.getId());
        if (shipCount >= portLevel) {
            throw new ApiException(
                    "Достигнут лимит кораблей для уровня порта " + portLevel + ". Улучшите порт.",
                    HttpStatus.BAD_REQUEST);
        }

        ShipTypeDto config = shipTypesMap.get(request.getShipTypeKey());
        if (config == null) {
            throw new ApiException("Неизвестный тип корабля: " + request.getShipTypeKey(), HttpStatus.BAD_REQUEST);
        }

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        if ("CRYSTALS".equalsIgnoreCase(request.getPaymentType())) {
            if (resources.getCrystals() < config.getCrystalsCost()) {
                throw new ApiException("Недостаточно кристаллов. Нужно: " + config.getCrystalsCost(), HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), 0, 0, 0, -config.getCrystalsCost());
        } else {
            if (resources.getGold() < config.getGoldCost() || resources.getWood() < config.getWoodCost()) {
                throw new ApiException(
                        String.format("Недостаточно ресурсов. Нужно: %d золота и %d дерева.",
                                config.getGoldCost(), config.getWoodCost()),
                        HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), -config.getGoldCost(), -config.getWoodCost(), 0, 0);
        }

        UserShip ship = UserShip.builder()
                .owner(user)
                .shipTypeKey(config.getShipTypeKey())
                .name(config.getName())
                .level(1)
                .capacity(config.getBaseCapacity())
                .mode(ShipMode.RESERVE)
                .build();

        UserShip saved = shipRepository.save(ship);
        log.info("User {} bought ship '{}' (id={})", user.getId(), saved.getName(), saved.getId());

        // Триггерим квест first_ship
        userProgressService.handleAction(user, QuestTriggerAction.BUILD_SHIP);

        return toDto(saved);
    }

    @Transactional
    public UserShipDto setShipMode(User user, Long shipId, SetShipModeRequest request) {
        UserShip ship = shipRepository.findByIdAndOwnerId(shipId, user.getId())
                .orElseThrow(() -> new ApiException("Корабль не найден.", HttpStatus.NOT_FOUND));

        ShipMode newMode = request.getMode();

        // Только один корабль может быть в режиме ATTACK или DEFENSE
        if (newMode == ShipMode.ATTACK || newMode == ShipMode.DEFENSE) {
            shipRepository.findByOwnerIdAndMode(user.getId(), newMode).ifPresent(existing -> {
                if (!existing.getId().equals(shipId)) {
                    existing.setMode(ShipMode.RESERVE);
                    shipRepository.save(existing);
                    log.info("Ship {} moved to RESERVE (replaced by ship {})", existing.getId(), shipId);
                }
            });
        }

        ship.setMode(newMode);
        return toDto(shipRepository.save(ship));
    }

    @Transactional
    public UserShipDto assignUnitToShip(User user, Long shipId, AssignUnitToShipRequest request) {
        UserShip ship = shipRepository.findByIdAndOwnerId(shipId, user.getId())
                .orElseThrow(() -> new ApiException("Корабль не найден.", HttpStatus.NOT_FOUND));

        Unit unit = unitRepository.findByIdAndOwnerId(request.getUnitId(), user.getId())
                .orElseThrow(() -> new ApiException("Юнит не найден.", HttpStatus.NOT_FOUND));

        if (!unit.isAlive()) {
            throw new ApiException("Нельзя посадить мёртвого юнита на корабль.", HttpStatus.BAD_REQUEST);
        }

        long crewCount = unitRepository.countByShipId(shipId);
        if (crewCount >= ship.getCapacity()) {
            throw new ApiException(
                    "Корабль заполнен. Вместимость: " + ship.getCapacity(),
                    HttpStatus.BAD_REQUEST);
        }

        // Снимаем с предыдущего корабля если был
        if (unit.getShip() != null) {
            unit.setShip(null);
            unitRepository.save(unit);
        }

        unit.setShip(ship);
        unitRepository.save(unit);
        log.info("Unit {} assigned to ship {}", unit.getId(), shipId);

        // Триггерим квест arena_fight
        userProgressService.handleAction(user, QuestTriggerAction.ASSIGN_TO_SHIP);

        return toDto(shipRepository.findByIdAndOwnerId(shipId, user.getId()).orElseThrow());
    }

    @Transactional
    public void removeUnitFromShip(User user, Long shipId, Long unitId) {
        Unit unit = unitRepository.findByIdAndOwnerId(unitId, user.getId())
                .orElseThrow(() -> new ApiException("Юнит не найден.", HttpStatus.NOT_FOUND));

        if (unit.getShip() == null || !unit.getShip().getId().equals(shipId)) {
            throw new ApiException("Юнит не находится на этом корабле.", HttpStatus.BAD_REQUEST);
        }

        unit.setShip(null);
        unitRepository.save(unit);
        log.info("Unit {} removed from ship {}", unitId, shipId);
    }

    @Transactional
    public UserShipDto upgradeShip(User user, Long shipId) {
        UserShip ship = shipRepository.findByIdAndOwnerId(shipId, user.getId())
                .orElseThrow(() -> new ApiException("Корабль не найден.", HttpStatus.NOT_FOUND));

        ShipTypeDto config = shipTypesMap.get(ship.getShipTypeKey());
        if (ship.getLevel() >= config.getMaxLevel()) {
            throw new ApiException("Корабль уже максимального уровня.", HttpStatus.BAD_REQUEST);
        }

        // Стоимость улучшения = базовая стоимость * уровень
        long goldCost = config.getGoldCost() * ship.getLevel();
        long woodCost = config.getWoodCost() * ship.getLevel();

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        if (resources.getGold() < goldCost || resources.getWood() < woodCost) {
            throw new ApiException(
                    String.format("Недостаточно ресурсов. Нужно: %d золота и %d дерева.", goldCost, woodCost),
                    HttpStatus.BAD_REQUEST);
        }

        resourcesRepository.addResources(user.getId(), -goldCost, -woodCost, 0, 0);

        ship.setLevel(ship.getLevel() + 1);
        ship.setCapacity(config.getBaseCapacity() + config.getCapacityPerLevel() * (ship.getLevel() - 1));
        UserShip saved = shipRepository.save(ship);

        log.info("Ship {} upgraded to level {}, capacity {}", shipId, saved.getLevel(), saved.getCapacity());

        // Триггерим квест upgrade_the_boat
        userProgressService.handleAction(user, QuestTriggerAction.UPGRADE_BUILDING);

        return toDto(saved);
    }

    private UserShipDto toDto(UserShip ship) {
        List<Unit> crew = unitRepository.findByShipId(ship.getId());
        List<UnitSummaryDto> crewDtos = crew.stream()
                .map(u -> UnitSummaryDto.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .level(u.getLevel())
                        .isAlive(u.isAlive())
                        .recoveryEndsAt(u.getRecoveryEndsAt())
                        .build())
                .collect(Collectors.toList());

        return UserShipDto.builder()
                .id(ship.getId())
                .shipTypeKey(ship.getShipTypeKey())
                .name(ship.getName())
                .level(ship.getLevel())
                .capacity(ship.getCapacity())
                .crewCount(crewDtos.size())
                .mode(ship.getMode())
                .crew(crewDtos)
                .build();
    }
}