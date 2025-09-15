package org.example.piratelegacy.auth.service;

import org.example.piratelegacy.auth.dto.BattleLogEntryDto;
import org.example.piratelegacy.auth.dto.BattleLogResultDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.entity.BattlePirate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Service
public class BattleService {

    private final Random random = new Random();

    public BattleLogResultDto fight(List<BattlePirateDto> pirateDtos) {
        List<BattlePirate> allies = new ArrayList<>();
        List<BattlePirate> enemies = new ArrayList<>();

        for (BattlePirateDto dto : pirateDtos) {
            BattlePirate bp = new BattlePirate(dto);
            if ("ALLY".equals(dto.getTeam())) {
                allies.add(bp);
            } else {
                enemies.add(bp);
            }
        }

        List<BattleLogEntryDto> log = new ArrayList<>();

        while (!allies.isEmpty() && !enemies.isEmpty()) {
            // союзники атакуют
            for (BattlePirate ally : allies) {
                if (enemies.isEmpty()) break;

                BattlePirate target = enemies.get(random.nextInt(enemies.size()));
                int dmg = random.nextInt(ally.getMaxAttack() - ally.getMinAttack() + 1) + ally.getMinAttack();
                dmg = Math.max(0, dmg - target.getArmor());

                target.applyDamage(dmg);
                boolean dead = target.isDead();
                if (dead) enemies.remove(target);

                log.add(new BattleLogEntryDto(ally.getId(), target.getId(), dmg, dead));
            }

            // враги атакуют
            for (BattlePirate enemy : enemies) {
                if (allies.isEmpty()) break;

                BattlePirate target = allies.get(random.nextInt(allies.size()));
                int dmg = random.nextInt(enemy.getMaxAttack() - enemy.getMinAttack() + 1) + enemy.getMinAttack();
                dmg = Math.max(0, dmg - target.getArmor());

                target.applyDamage(dmg);
                boolean dead = target.isDead();
                if (dead) allies.remove(target);

                log.add(new BattleLogEntryDto(enemy.getId(), target.getId(), dmg, dead));
            }
        }

        String winner = allies.isEmpty() ? "ENEMY" : "ALLY";

        // формируем список выживших для ответа
        List<BattlePirateDto> surviving = new ArrayList<>();
        for (BattlePirate ally : allies) {
            surviving.add(new BattlePirateDto(
                    ally.getId(), ally.getTeam(), ally.getHp(),
                    ally.getMinAttack(), ally.getMaxAttack(),
                    ally.getArmor(), 0, 0, 0, null
            ));
        }
        for (BattlePirate enemy : enemies) {
            surviving.add(new BattlePirateDto(
                    enemy.getId(), enemy.getTeam(), enemy.getHp(),
                    enemy.getMinAttack(), enemy.getMaxAttack(),
                    enemy.getArmor(), 0, 0, 0, null
            ));
        }

        return new BattleLogResultDto(winner, log, surviving);
    }
}