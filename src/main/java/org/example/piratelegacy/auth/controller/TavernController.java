package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.TavernUnitDto;
import org.example.piratelegacy.auth.dto.UnitSummaryDto;
import org.example.piratelegacy.auth.dto.request.HireUnitRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.TavernService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tavern")
@RequiredArgsConstructor
public class TavernController {

    private final TavernService tavernService;

    /**
     * Список доступных юнитов для найма.
     * GET /api/tavern/units
     */
    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<TavernUnitDto>>> getAvailableUnits() {
        return ResponseEntity.ok(new ApiResponse<>(true, tavernService.getAvailableUnits()));
    }

    /**
     * Нанять юнита.
     * POST /api/tavern/hire
     * Body: { "unitTypeKey": "pirate", "paymentType": "GOLD" }
     */
    @PostMapping("/hire")
    public ResponseEntity<ApiResponse<UnitSummaryDto>> hireUnit(
            @CurrentUser User user,
            @RequestBody HireUnitRequest request) {
        UnitSummaryDto hired = tavernService.hireUnit(user, request);
        return ResponseEntity.ok(new ApiResponse<>(true, hired));
    }
}