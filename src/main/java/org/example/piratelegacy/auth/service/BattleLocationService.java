package org.example.piratelegacy.auth.service;

import org.example.piratelegacy.auth.dto.BattleLocationDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BattleLocationService {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;

    private static final int ALLY_COUNT = 6;
    private static final int ENEMY_COUNT = 4;

    private static final int BASE_HP = 100;
    private static final int MIN_ATTACK = 10;
    private static final int MAX_ATTACK = 20;
    private static final int ARMOR = 1;
    private static final int ALLY_XP = 200;
    private static final int ENEMY_XP = 120;

    public BattleLocationDto generateFirstQuestLocation() {
        List<BattlePirateDto> pirates = new ArrayList<>();

        // создаем союзников
        for (int i = 0; i < ALLY_COUNT; i++) {
            pirates.add(generatePirate("ALLY", ALLY_XP, "ally_pirate_01"));
        }

        // создаем врагов
        for (int i = 0; i < ENEMY_COUNT; i++) {
            pirates.add(generatePirate("ENEMY", ENEMY_XP, "enemy_pirate_01"));
        }

        return new BattleLocationDto("battle_location_01", pirates);
    }

    private BattlePirateDto generatePirate(String team, int xp, String imageId) {
        int x = (int)(Math.random() * WIDTH);
        int y = (int)(Math.random() * HEIGHT);
        return new BattlePirateDto(UUID.randomUUID().toString(), team, BASE_HP, MIN_ATTACK, MAX_ATTACK, ARMOR, xp, x, y, imageId);
    }
}