package org.example.piratelegacy.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quest_chain_id", "quest_order"})
})
public class Quest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_chain_id", nullable = false)
    private QuestChain questChain;

    @Column(name = "quest_key", nullable = false, unique = true)
    private String questKey;

    @Column(name = "quest_order", nullable = false)
    private Integer questOrder;

    @Column(nullable = false)
    private String title;

    @Column(name = "npc_name")
    private String npcName;

    @Column(name = "npc_image_url")
    private String npcImageUrl;

    @Column(name = "story_text", columnDefinition = "TEXT")
    private String storyText;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column(name = "gold_reward")
    private Long goldReward;

    @Column(name = "exp_reward")
    private Long expReward;

    @Column(name = "wood_reward")
    private Long woodReward;

    @Column(name = "stone_reward")
    private Long stoneReward;

    @Column(name = "crystals_reward")
    private Long crystalsReward;

    @Column(name = "button_text")
    private String buttonText;

    @Column(name = "battle_location_id")
    private String battleLocationId;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestItemReward> itemRewards;
}