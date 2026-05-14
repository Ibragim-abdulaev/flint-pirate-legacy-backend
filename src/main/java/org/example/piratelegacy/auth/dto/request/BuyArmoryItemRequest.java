package org.example.piratelegacy.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyArmoryItemRequest {
    private String itemKey;
    private String paymentType;
}