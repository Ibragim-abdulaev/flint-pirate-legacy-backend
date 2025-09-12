package org.example.piratelegacy.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_pirates")
public class Pirate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private String pirateClass; // "MELEE" или "RANGED"

    @Column(nullable = false)
    private String characterType; // тип персонажа (BARBARIAN, ARCHER, и т.д.)

    @Column(nullable = false)
    private int minAttack = 10;

    @Column(nullable = false)
    private int maxAttack = 15;

    @Column(nullable = false)
    private int hp = 100;

    @Column(nullable = false)
    private int armor = 0;

    @Column(name = "special_ability")
    private String specialAbility; // описание спецспособности

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;
}