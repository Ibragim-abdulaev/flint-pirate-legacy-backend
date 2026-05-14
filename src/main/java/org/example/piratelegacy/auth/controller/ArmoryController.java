package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.ArmoryItemDto;
import org.example.piratelegacy.auth.dto.request.BuyArmoryItemRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.ArmoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/armory")
@RequiredArgsConstructor
public class ArmoryController {

    private final ArmoryService armoryService;

    @PostMapping("/build")
    public ResponseEntity<ApiResponse<Void>> buildArmory(@CurrentUser User user) {
        armoryService.buildArmory(user);
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<ArmoryItemDto>>> getItems(@CurrentUser User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, armoryService.getAvailableItems(user)));
    }

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<Void>> buyItem(
            @CurrentUser User user,
            @RequestBody BuyArmoryItemRequest request) {
        armoryService.buyItem(user, request);
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }
}