package org.example.piratelegacy.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.enums.ShipMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetShipModeRequest {
    private ShipMode mode;
}