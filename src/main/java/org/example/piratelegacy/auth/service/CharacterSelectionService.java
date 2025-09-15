package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.CharacterSelectionRequest;
import org.example.piratelegacy.auth.entity.CharacterType;
import org.example.piratelegacy.auth.entity.Hero;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.HeroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterSelectionService {

    private final HeroRepository heroRepository;

    @Transactional
    public Hero selectCharacter(User user, CharacterSelectionRequest request) {
        // Проверяем, что у пользователя еще нет персонажа
        if (!heroRepository.findByOwnerId(user.getId()).isEmpty()) {
            throw new RuntimeException("У пользователя уже есть персонаж!");
        }

        // Валидация имени
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Имя персонажа не может быть пустым");
        }

        if (request.getName().length() > 50) {
            throw new RuntimeException("Имя персонажа не может быть длиннее 50 символов");
        }

        // Получаем тип персонажа
        CharacterType characterType;
        try {
            characterType = CharacterType.valueOf(request.getCharacterType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Неверный тип персонажа: " + request.getCharacterType());
        }

        // Создаем героя с характеристиками выбранного типа
        Hero hero = Hero.builder()
                .name(request.getName().trim())
                .level(1)
                .heroClass(characterType.getCombatClass())
                .characterType(characterType.name())
                .minAttack(characterType.getMinAttack())
                .maxAttack(characterType.getMaxAttack())
                .hp(characterType.getBaseHp())
                .armor(characterType.getBaseArmor())
                .specialAbility(characterType.getSpecialAbility())
                .owner(user)
                .build();

        return heroRepository.save(hero);
    }

    @Transactional(readOnly = true)
    public boolean userHasCharacter(User user) {
        return !heroRepository.findByOwnerId(user.getId()).isEmpty();
    }

    @Transactional(readOnly = true)
    public Hero getUserCharacter(User user) {
        List<Hero> heroes = heroRepository.findByOwnerId(user.getId());
        if (heroes.isEmpty()) {
            throw new RuntimeException("У пользователя нет персонажа");
        }
        return heroes.get(0);
    }
}