package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.ShipMode;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserShipDto implements Serializable {
    private Long id;
    private String shipTypeKey;
    private String name;
    private int level;
    private int capacity;
    private int crewCount;
    private ShipMode mode;
    private List<UnitSummaryDto> crew;
}