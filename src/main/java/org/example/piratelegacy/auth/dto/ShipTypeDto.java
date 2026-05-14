package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipTypeDto implements Serializable {
    private String shipTypeKey;
    private String name;
    private String description;
    private int baseCapacity;
    private int maxLevel;
    private int capacityPerLevel;
    private long goldCost;
    private long woodCost;
    private long stoneCost;
    private long crystalsCost;
}