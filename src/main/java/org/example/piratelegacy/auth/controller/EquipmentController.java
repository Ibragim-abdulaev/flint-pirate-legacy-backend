package org.example.piratelegacy.auth.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.enums.ItemType;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping("/equip")
    public ResponseEntity<ApiResponse<MessageResponse>> equipItem(
            @CurrentUser User user,
            @RequestBody EquipRequest request) {
        equipmentService.equipItem(user, request.getUnitId(), request.getInventoryItemId());
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Предмет экипирован.")));
    }

    @PostMapping("/unequip")
    public ResponseEntity<ApiResponse<MessageResponse>> unequipItem(
            @CurrentUser User user,
            @RequestBody UnequipRequest request) {
        equipmentService.unequipItem(user, request.getUnitId(), request.getItemType());
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Предмет снят.")));
    }

    @Data
    private static class EquipRequest {
        private Long unitId;
        private Long inventoryItemId;
    }

    @Data
    private static class UnequipRequest {
        private Long unitId;
        private ItemType itemType;
    }
}