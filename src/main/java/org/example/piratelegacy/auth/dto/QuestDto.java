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
    // Основная информация о квесте
    private String questKey;
    private String title;
    private String npcName;         // Имя главного NPC квеста для аватара
    private String npcImageUrl;     // URL его аватара

    // Разделенные текст и цель
    private String storyText;       // Сюжетное описание, диалоги
    private String objective;       // Краткая цель задания (жирным шрифтом)

    // Награды
    private Long goldReward;
    private Long expReward;
    private List<ItemRewardDto> itemRewards; // Список наград-предметов

    // Техническая информация
    private String buttonText;
    private String battleLocationId;
}