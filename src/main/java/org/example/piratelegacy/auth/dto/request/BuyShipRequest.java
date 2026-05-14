package org.example.piratelegacy.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyShipRequest {
    private String shipTypeKey;
    // "GOLD" или "CRYSTALS"
    private String paymentType;
}