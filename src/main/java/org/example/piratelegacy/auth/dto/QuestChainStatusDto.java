package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestChainStatusDto implements Serializable {
    private String chainKey;
    private String title;
    private String iconUrl;
    private String journalImageUrl;
    private int completedSteps;
    private int totalSteps;
    private boolean isAvailable;
    private boolean isCompleted;
    private QuestDto currentQuest;
}