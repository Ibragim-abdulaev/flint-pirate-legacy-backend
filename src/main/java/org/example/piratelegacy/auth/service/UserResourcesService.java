package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserResources;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserResourcesService {

    private final UserResourcesRepository repository;

    @Transactional
    public UserResources createInitialResources(User user) {
        UserResources resources = UserResources.builder()
                .user(user)
                .gold(100L)
                .wood(50L)
                .stone(50L)
                .build();
        log.info("Created initial resources for new user {}", user.getId());
        return repository.save(resources);
    }

    @Transactional
    public void addResources(User user, Long gold, Long wood, Long stone) {
        if (gold == null && wood == null && stone == null) return;

        long g = gold != null ? gold : 0L;
        long w = wood != null ? wood : 0L;
        long s = stone != null ? stone : 0L;

        if (g == 0 && w == 0 && s == 0) return;

        repository.addResources(user.getId(), g, w, s);
        log.info("Added resources for user {}: gold={}, wood={}, stone={}", user.getId(), g, w, s);
    }

    @Transactional(readOnly = true)
    public UserResourcesDto getResources(User user) {
        return repository.findByUserId(user.getId())
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User resources not found for user " + user.getId()));
    }

    private UserResourcesDto toDto(UserResources entity) {
        return new UserResourcesDto(entity.getGold(), entity.getWood(), entity.getStone());
    }
}