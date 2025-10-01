package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor // <-- УБЕДИТЕСЬ, ЧТО ЭТА АННОТАЦИЯ ЕСТЬ
@NoArgsConstructor
@Builder
public class QuestChainStatusDto implements Serializable {
    private String chainKey;
    private String title;
    private String iconUrl;
    private int completedSteps;
    private int totalSteps;
    private boolean isAvailable;
    private boolean isCompleted;
    private QuestDto currentQuest;
}