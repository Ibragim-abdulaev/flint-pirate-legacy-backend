package org.example.piratelegacy.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HireUnitRequest {
    // Ключ типа юнита из tavern_units.json
    private String unitTypeKey;
    // Способ оплаты: "GOLD" или "CRYSTALS"
    private String paymentType;
}