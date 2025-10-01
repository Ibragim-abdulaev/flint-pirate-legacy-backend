package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.TeamType;

import java.io.Serializable;
import java.util.List;

/**
 * DTO для отправки на фронтенд полных результатов завершенного боя.
 * Содержит информацию о победителе, наградах и пошаговый лог для анимации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultDto implements Serializable {

    /**
     * Команда, победившая в бою (ALLY или ENEMY).
     */
    private TeamType winnerTeam;

    /**
     * Объект с наградами, которые получает игрок в случае победы.
     */
    private RewardsDto rewards;

    /**
     * Пошаговый "сценарий" всего боя для воспроизведения анимаций на фронтенде.
     */
    private List<BattleLogEntryDto> log;

    /**
     * Список ID павших юнитов игрока.
     */
    private List<String> yourLossesIds;

    /**
     * Список ID павших вражеских юнитов.
     */
    private List<String> enemyLossesIds;


    /**
     * Вложенный DTO для описания наград за бой.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardsDto implements Serializable {
        private long experience;
        private long gold;
        private long wood;
        private long stone;
        private List<ItemRewardDto> items;
    }
}