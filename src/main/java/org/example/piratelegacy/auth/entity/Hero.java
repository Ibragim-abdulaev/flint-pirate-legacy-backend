package org.example.piratelegacy.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.piratelegacy.auth.entity.enums.CombatClass;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_heroes")
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CombatClass heroClass;

    @Column(nullable = false)
    private String characterType;

    @Column(nullable = false)
    private int minAttack = 10;

    @Column(nullable = false)
    private int maxAttack = 15;

    @Column(nullable = false)
    private int hp = 100;

    @Column(nullable = false)
    private int armor = 0;

    @Column(name = "special_ability")
    private String specialAbility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;
}