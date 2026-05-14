package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeadPirateDto implements Serializable {
    private Long id;
    private String name;
    private int level;
    private String unitTypeKey;
    private long reviveGoldCost;
    private long reviveCrystalsCost;
}