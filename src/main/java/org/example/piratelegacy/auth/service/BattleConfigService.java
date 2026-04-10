package org.example.piratelegacy.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.LocationConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleConfigService {

    private final ObjectMapper objectMapper;
    private final Map<String, LocationConfig> locationConfigs = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadBattleConfigs() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:battles/*.json");

        for (Resource resource : resources) {
            try (InputStream inputStream = resource.getInputStream()) {
                LocationConfig config = objectMapper.readValue(inputStream, LocationConfig.class);
                String locationId = Objects.requireNonNull(resource.getFilename()).replace(".json", "");
                locationConfigs.put(locationId, config);
            }
        }
    }

    public LocationConfig getLocationConfig(String locationId) {
        LocationConfig config = locationConfigs.get(locationId);
        if (config == null) {
            throw new IllegalArgumentException("Конфигурация для локации не найдена: " + locationId);
        }
        return config;
    }
}