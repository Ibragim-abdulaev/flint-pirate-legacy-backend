package org.example.piratelegacy.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_units")
public class Unit implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @Column(name = "unit_type_key", nullable = false)
    private String unitTypeKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private long experience = 0L;

    @Column(name = "base_hp")
    private int baseHp;

    @Column(name = "base_min_attack")
    private int baseMinAttack;

    @Column(name = "base_max_attack")
    private int baseMaxAttack;

    @Column(name = "base_armor")
    private int baseArmor;

    @Column(name = "is_main_hero", nullable = false)
    @Builder.Default
    private Boolean isMainHero = false;

    @Column(name = "is_alive", nullable = false)
    @Builder.Default
    private boolean isAlive = true;

    @Column(name = "recovery_ends_at")
    private LocalDateTime recoveryEndsAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_weapon_id")
    private InventoryItem equippedWeapon;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_armor_id")
    private InventoryItem equippedArmor;
}