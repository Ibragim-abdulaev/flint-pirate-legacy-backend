package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QuestJsonDto {
    private String title;
    private String description;
    private String npcName;
    private String npcImageUrl;
    private Long goldReward;
    private Long expReward;
    private String buttonText;
    private Integer questOrder;
    private boolean isActive;
}