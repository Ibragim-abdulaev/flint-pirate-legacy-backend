package org.example.piratelegacy.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestCompleteResponse {
    private String questKey;
    private Long goldReward;
    private Long woodReward;
    private Long stoneReward;
    private Long expReward;

    // Текущий уровень острова после начисления опыта
    private int currentPlayerLevel;

    // XP до следующего уровня острова
    private long playerXpToNextLevel;
}