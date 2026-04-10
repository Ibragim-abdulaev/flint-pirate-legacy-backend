package org.example.piratelegacy.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.piratelegacy.auth.entity.enums.QuestChainType;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quest_chains")
public class QuestChain implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chain_key", nullable = false, unique = true)
    private String chainKey;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "journal_icon_url")
    private String journalImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "chain_type", nullable = false)
    private QuestChainType chainType;

    @OneToMany(mappedBy = "questChain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("questOrder ASC")
    private List<Quest> quests;
}