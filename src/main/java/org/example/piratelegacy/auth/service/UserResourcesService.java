package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserResources;
import org.example.piratelegacy.auth.repository.UserRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserResourcesService {

    private final UserResourcesRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public UserResources createInitialResources(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResources resources = new UserResources();
        resources.setUser(user);
        resources.setGold(10000L);
        resources.setWood(10000L);
        resources.setStone(10000L);
        resources.setCrystals(3000L);
        log.info("Created initial resources for new user {}", userId);
        return repository.save(resources);
    }

    @Transactional
    public void addResources(User user, Long gold, Long wood, Long stone, Long crystals) {
        if (gold == null && wood == null && stone == null && crystals == null) return;

        long g = gold != null ? gold : 0L;
        long w = wood != null ? wood : 0L;
        long s = stone != null ? stone : 0L;
        long c = crystals != null ? crystals : 0L;

        if (g == 0 && w == 0 && s == 0 && c == 0) return;

        repository.addResources(user.getId(), g, w, s, c);
        log.info("Added resources for user {}: gold={}, wood={}, stone={}, crystals={}", user.getId(), g, w, s, c);
    }

    @Transactional(readOnly = true)
    public UserResourcesDto getResources(User user) {
        return repository.findByUserId(user.getId())
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User resources not found for user " + user.getId()));
    }

    private UserResourcesDto toDto(UserResources entity) {
        return new UserResourcesDto(entity.getGold(), entity.getWood(), entity.getStone(), entity.getCrystals());
    }
}
