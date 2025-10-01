package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.UnitProfileDto;
import org.example.piratelegacy.auth.dto.UnitSummaryDto;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.UnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final CharacterSelectionService characterSelectionService;

    /**
     * Возвращает краткую информацию о всех юнитах в команде.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UnitSummaryDto>>> getTeamSummary(@CurrentUser User user) {
        List<UnitSummaryDto> team = unitService.getTeamSummary(user);
        return ResponseEntity.ok(new ApiResponse<>(true, team));
    }

    /**
     * Возвращает полный профиль главного героя пользователя.
     */
    @GetMapping("/main")
    public ResponseEntity<ApiResponse<UnitProfileDto>> getMainUnitProfile(@CurrentUser User user) {
        Unit mainHero = characterSelectionService.getUserCharacter(user);
        UnitProfileDto profile = unitService.getUnitProfile(mainHero.getId(), user);
        return ResponseEntity.ok(new ApiResponse<>(true, profile));
    }

    /**
     * Возвращает полный профиль конкретного юнита по его ID.
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<ApiResponse<UnitProfileDto>> getUnitProfile(
            @CurrentUser User user,
            @PathVariable Long unitId) {
        UnitProfileDto profile = unitService.getUnitProfile(unitId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, profile));
    }
}