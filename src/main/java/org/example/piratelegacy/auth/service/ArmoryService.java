
package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.ArmoryItemDto;
import org.example.piratelegacy.auth.dto.request.BuyArmoryItemRequest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserBuilding;
import org.example.piratelegacy.auth.entity.enums.BuildingType;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UserBuildingRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
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
public class ArmoryService {

    private final UserBuildingRepository buildingRepository;
    private final UserResourcesRepository resourcesRepository;
    private final UserProgressService userProgressService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${game.config.armory-items-path:classpath:game/armory_items.json}")
    private String armoryItemsPath;

    private Map<String, ArmoryItemDto> armoryItemsMap;

    @PostConstruct
    public void init() {
        try (InputStream is = resourceLoader.getResource(armoryItemsPath).getInputStream()) {
            List<ArmoryItemDto> items = objectMapper.readValue(is, new TypeReference<>() {});
            armoryItemsMap = items.stream()
                    .collect(Collectors.toUnmodifiableMap(ArmoryItemDto::getItemKey, Function.identity()));
            log.info("Loaded {} armory items", armoryItemsMap.size());
        } catch (IOException e) {
            log.error("Failed to load armory items", e);
            armoryItemsMap = Collections.emptyMap();
        }
    }

    public List<ArmoryItemDto> getAvailableItems(User user) {
        assertArmoryBuilt(user);
        return List.copyOf(armoryItemsMap.values());
    }

    @Transactional
    public void buildArmory(User user) {
        if (buildingRepository.existsByOwnerIdAndBuildingType(user.getId(), BuildingType.ARMORY)) {
            throw new ApiException("Оружейная уже построена.", HttpStatus.BAD_REQUEST);
        }

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        long goldCost = 500, woodCost = 300;

        if (resources.getGold() < goldCost || resources.getWood() < woodCost) {
            throw new ApiException(
                    String.format("Недостаточно ресурсов. Нужно: %d золота и %d дерева.", goldCost, woodCost),
                    HttpStatus.BAD_REQUEST);
        }

        resourcesRepository.addResources(user.getId(), -goldCost, -woodCost, 0, 0);

        buildingRepository.save(UserBuilding.builder()
                .owner(user)
                .buildingType(BuildingType.ARMORY)
                .level(1)
                .build());

        log.info("User {} built ARMORY", user.getId());

        // Триггерим квест weapons
        userProgressService.handleAction(user, QuestTriggerAction.UPGRADE_BUILDING);
    }

    @Transactional
    public void buyItem(User user, BuyArmoryItemRequest request) {
        assertArmoryBuilt(user);

        ArmoryItemDto item = armoryItemsMap.get(request.getItemKey());
        if (item == null) {
            throw new ApiException("Неизвестный товар: " + request.getItemKey(), HttpStatus.BAD_REQUEST);
        }

        var resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы не найдены.", HttpStatus.INTERNAL_SERVER_ERROR));

        if ("CRYSTALS".equalsIgnoreCase(request.getPaymentType())) {
            if (resources.getCrystals() < item.getCrystalsCost()) {
                throw new ApiException("Недостаточно кристаллов. Нужно: " + item.getCrystalsCost(), HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), 0, 0, 0, -item.getCrystalsCost());
        } else {
            if (resources.getGold() < item.getGoldCost()) {
                throw new ApiException("Недостаточно золота. Нужно: " + item.getGoldCost(), HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), -item.getGoldCost(), 0, 0, 0);
        }

        log.info("User {} bought armory item '{}'", user.getId(), item.getName());

        // Триггерим квест hidden_advantage
        userProgressService.handleAction(user, QuestTriggerAction.BUY_ITEM);
    }

    private void assertArmoryBuilt(User user) {
        buildingRepository.findByOwnerIdAndBuildingType(user.getId(), BuildingType.ARMORY)
                .orElseThrow(() -> new ApiException("Оружейная ещё не построена.", HttpStatus.BAD_REQUEST));
    }
}