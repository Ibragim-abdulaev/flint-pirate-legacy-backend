package org.example.piratelegacy.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "npc_name", nullable = false)
    private String npcName;

    @Column(name = "npc_image_url")
    private String npcImageUrl;

    @Column(name = "gold_reward")
    private Long goldReward = 0L;

    @Column(name = "exp_reward")
    private Long expReward = 0L;

    @Column(name = "button_text")
    private String buttonText = "В путь";

    @Column(name = "quest_order", nullable = false)
    private Integer questOrder; // порядок квестов

    @Column(nullable = false)
    private boolean isActive = true;
}
