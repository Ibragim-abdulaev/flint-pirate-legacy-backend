package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.Quest;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestDto implements Serializable {
    private String questKey;
    private String title;
    private String npcName;
    private String npcImageUrl;
    private String storyText;
    private String objective;
    private Long goldReward;
    private Long wood;
    private Long stone;
    private Long crystals;
    private Long expReward;
    private List<ItemRewardDto> itemRewards;
    private String buttonText;
    private String battleLocationId;
}