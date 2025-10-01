package org.example.piratelegacy.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.piratelegacy.auth.entity.enums.ItemType;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "items")
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, unique = true)
    private String itemKey;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    // Оставляем только тип предмета (оружие или броня)
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    // Бонусы к характеристикам
    private int bonusHp = 0;
    private int bonusDamage = 0;
    private int bonusArmor = 0;
}