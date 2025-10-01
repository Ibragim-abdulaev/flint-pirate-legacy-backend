package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.CharacterStatsDto;
import org.example.piratelegacy.auth.entity.enums.CharacterType;
import org.example.piratelegacy.auth.entity.enums.CombatClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameConfigService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${game.config.characters-path}")
    private final String charactersPath;

    @Value("${game.config.starters-path}")
    private final String startersPath;

    private Map<CharacterType, CharacterStats> characterStatsMap;
    private Set<CharacterType> starterCharacterTypes;

    public GameConfigService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${game.config.characters-path}") String charactersPath,
            @Value("${game.config.starters-path}") String startersPath
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.charactersPath = charactersPath;
        this.startersPath = startersPath;
    }

    @PostConstruct
    public void init() {
        loadCharacterConfigs();
        loadStarterConfigs();
    }

    private void loadCharacterConfigs() {
        try (InputStream inputStream = resourceLoader.getResource(charactersPath).getInputStream()) {
            List<CharacterStatsDto> statsList = objectMapper.readValue(inputStream, new TypeReference<>() {});
            this.characterStatsMap = statsList.stream()
                    .map(CharacterStats::fromDto)
                    .collect(Collectors.toUnmodifiableMap(CharacterStats::getCharacterType, Function.identity()));
            log.info("Successfully loaded {} character configurations.", characterStatsMap.size());
        } catch (IOException e) {
            log.error("Failed to load character configurations from {}", charactersPath, e);
            this.characterStatsMap = Collections.emptyMap();
        }
    }

    private void loadStarterConfigs() {
        try (InputStream inputStream = resourceLoader.getResource(startersPath).getInputStream()) {
            List<String> starterNames = objectMapper.readValue(inputStream, new TypeReference<>() {});
            this.starterCharacterTypes = starterNames.stream()
                    .map(CharacterType::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
            log.info("Successfully loaded {} starter character types.", starterCharacterTypes.size());
        } catch (IOException e) {
            log.error("Failed to load starter character configurations from {}", startersPath, e);
            this.starterCharacterTypes = Collections.emptySet();
        }
    }

    public CharacterStats getCharacterStats(CharacterType type) {
        return characterStatsMap.get(type);
    }

    public List<CharacterStats> getStarterCharacters() {
        if (starterCharacterTypes == null || characterStatsMap == null) {
            return Collections.emptyList();
        }
        return starterCharacterTypes.stream()
                .map(this::getCharacterStats)
                .collect(Collectors.toList());
    }

    @Data
    public static class CharacterStats implements Serializable {
        private CharacterType characterType;
        private String displayName;
        private CombatClass combatClass;
        private int baseHp;
        private int minAttack;
        private int maxAttack;
        private int baseArmor;
        private String specialAbility;

        public static CharacterStats fromDto(CharacterStatsDto dto) {
            CharacterStats stats = new CharacterStats();
            stats.setCharacterType(dto.getCharacterType());
            stats.setDisplayName(dto.getDisplayName());
            stats.setCombatClass(dto.getCombatClass());
            stats.setBaseHp(dto.getBaseHp());
            stats.setMinAttack(dto.getMinAttack());
            stats.setMaxAttack(dto.getMaxAttack());
            stats.setBaseArmor(dto.getBaseArmor());
            stats.setSpecialAbility(dto.getSpecialAbility());
            return stats;
        }
    }
}