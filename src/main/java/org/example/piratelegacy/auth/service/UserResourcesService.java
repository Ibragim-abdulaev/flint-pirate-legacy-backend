package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserResources;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return repository.save(resources);
    }

    @Transactional
    public UserResourcesDto addResources(User user, long gold, long wood, long stone) {
        UserResources resources = repository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("User resources not found"));

        resources.setGold(resources.getGold() + gold);
        resources.setWood(resources.getWood() + wood);
        resources.setStone(resources.getStone() + stone);

        return toDto(repository.save(resources));
    }

    @Transactional(readOnly = true)
    public UserResourcesDto getResources(User user) {
        return repository.findByUserId(user.getId())
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User resources not found"));
    }

    @Transactional
    public UserResourcesDto spendResources(User user, long gold, long wood, long stone) {
        UserResources resources = repository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("User resources not found"));

        if (resources.getGold() < gold || resources.getWood() < wood || resources.getStone() < stone) {
            throw new RuntimeException("Not enough resources");
        }

        resources.setGold(resources.getGold() - gold);
        resources.setWood(resources.getWood() - wood);
        resources.setStone(resources.getStone() - stone);

        return toDto(repository.save(resources));
    }

    private UserResourcesDto toDto(UserResources entity) {
        return new UserResourcesDto(entity.getGold(), entity.getWood(), entity.getStone());
    }
}