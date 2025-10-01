package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.InventoryItemDto;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.UserInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final UserInventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> getInventory(@CurrentUser User user) {
        List<InventoryItemDto> inventory = inventoryService.getInventory(user);
        return ResponseEntity.ok(new ApiResponse<>(true, inventory));
    }
}