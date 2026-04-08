package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryItemDto implements Serializable {
    private Long inventoryId;
    private String itemKey;
    private String name;
    private String imageUrl;
    private int quantity;

}