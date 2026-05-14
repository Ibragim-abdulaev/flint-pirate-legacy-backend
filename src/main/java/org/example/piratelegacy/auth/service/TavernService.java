package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.TavernUnitDto;
import org.example.piratelegacy.auth.dto.UnitSummaryDto;
import org.example.piratelegacy.auth.dto.request.HireUnitRequest;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserResources;
import org.example.piratelegacy.auth.entity.enums.QuestTriggerAction;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TavernService {

    private final UnitRepository unitRepository;
    private final UserResourcesRepository resourcesRepository;
    private final UserProgressService userProgressService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${game.config.tavern-units-path:classpath:game/tavern_units.json}")
    private String tavernUnitsPath;

    private Map<String, TavernUnitDto> tavernUnitsMap;

    @PostConstruct
    public void init() {
        try (InputStream is = resourceLoader.getResource(tavernUnitsPath).getInputStream()) {
            List<TavernUnitDto> units = objectMapper.readValue(is, new TypeReference<>() {});
            tavernUnitsMap = units.stream()
                    .collect(Collectors.toUnmodifiableMap(TavernUnitDto::getUnitTypeKey, Function.identity()));
            log.info("Loaded {} tavern unit types", tavernUnitsMap.size());
        } catch (IOException e) {
            log.error("Failed to load tavern units config", e);
            tavernUnitsMap = Collections.emptyMap();
        }
    }

    /**
     * Возвращает список доступных юнитов в трактире.
     */
    public List<TavernUnitDto> getAvailableUnits() {
        return List.copyOf(tavernUnitsMap.values());
    }

    /**
     * Нанимает юнита за золото или кристаллы.
     * После успешного найма проверяет и обновляет квестовый прогресс.
     */
    @Transactional
    public UnitSummaryDto hireUnit(User user, HireUnitRequest request) {
        TavernUnitDto config = tavernUnitsMap.get(request.getUnitTypeKey());
        if (config == null) {
            throw new ApiException("Неизвестный тип юнита: " + request.getUnitTypeKey(), HttpStatus.BAD_REQUEST);
        }

        UserResources resources = resourcesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Ресурсы пользователя не найдены", HttpStatus.INTERNAL_SERVER_ERROR));

        // Списываем ресурсы в зависимости от способа оплаты
        if ("CRYSTALS".equalsIgnoreCase(request.getPaymentType())) {
            if (resources.getCrystals() < config.getCrystalsCost()) {
                throw new ApiException("Недостаточно кристаллов. Нужно: " + config.getCrystalsCost(), HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), 0, 0, 0, -config.getCrystalsCost());
            log.info("User {} hired {} for {} crystals", user.getId(), config.getName(), config.getCrystalsCost());
        } else {
            // По умолчанию — золото
            if (resources.getGold() < config.getGoldCost()) {
                throw new ApiException("Недостаточно золота. Нужно: " + config.getGoldCost(), HttpStatus.BAD_REQUEST);
            }
            resourcesRepository.addResources(user.getId(), -config.getGoldCost(), 0, 0, 0);
            log.info("User {} hired {} for {} gold", user.getId(), config.getName(), config.getGoldCost());
        }

        // Создаём юнита в базе
        Unit unit = Unit.builder()
                .owner(user)
                .unitTypeKey(config.getUnitTypeKey())
                .name(config.getName())
                .level(1)
                .experience(0L)
                .baseHp(config.getBaseHp())
                .baseMinAttack(config.getBaseMinAttack())
                .baseMaxAttack(config.getBaseMaxAttack())
                .baseArmor(config.getBaseArmor())
                .isMainHero(false)
                .isAlive(true)
                .build();

        Unit saved = unitRepository.save(unit);
        log.info("Created unit id={} '{}' for user {}", saved.getId(), saved.getName(), user.getId());

        // Сообщаем системе квестов что произошло действие HIRE_UNIT
        userProgressService.handleAction(user, QuestTriggerAction.HIRE_UNIT);

        return UnitSummaryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .level(saved.getLevel())
                .isAlive(saved.isAlive())
                .build();
    }
}