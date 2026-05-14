package org.example.piratelegacy.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.piratelegacy.auth.entity.enums.BuildingType;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_buildings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "building_type"}))
public class UserBuilding implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", nullable = false)
    private BuildingType buildingType;

    @Column(nullable = false)
    @Builder.Default
    private int level = 1;
}