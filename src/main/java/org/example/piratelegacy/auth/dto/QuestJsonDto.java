package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestDto {
    private Long id;
    private String title;
    private String description;
    private String npcName;
    private String npcImageUrl;
    private boolean isCompleted;
    private Long goldReward;
    private Long expReward;
    private String buttonText;
}