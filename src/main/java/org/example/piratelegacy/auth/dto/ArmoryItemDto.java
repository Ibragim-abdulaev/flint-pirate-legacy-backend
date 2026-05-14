package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArmoryItemDto implements Serializable {
    private String itemKey;
    private String name;
    private String description;
    private long goldCost;
    private long crystalsCost;
}