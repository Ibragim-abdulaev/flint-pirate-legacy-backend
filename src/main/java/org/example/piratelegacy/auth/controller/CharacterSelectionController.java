package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.CharacterOptionDto;
import org.example.piratelegacy.auth.dto.UnitProfileDto;
import org.example.piratelegacy.auth.dto.request.CharacterSelectionRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.GameConfigService;
import org.example.piratelegacy.auth.service.UnitService; // <-- Убедитесь, что импорт есть
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/character")
@RequiredArgsConstructor
public class CharacterSelectionController {

    private final CharacterSelectionService characterSelectionService;
    private final GameConfigService gameConfigService;
    private final UnitService unitService; // <-- Убедитесь, что сервис добавлен

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<CharacterOptionDto>>> getCharacterOptions() {
        List<CharacterOptionDto> options = gameConfigService.getStarterCharacters().stream()
                .map(CharacterOptionDto::fromCharacterStats)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, options));
    }

    @PostMapping("/select")
    public ResponseEntity<ApiResponse<UnitProfileDto>> selectCharacter(
            @CurrentUser User user,
            @RequestBody CharacterSelectionRequest request) {

        Unit newHeroUnit = characterSelectionService.selectCharacter(user, request);

        UnitProfileDto profile = unitService.getUnitProfile(newHeroUnit.getId(), user);

        return ResponseEntity.ok(new ApiResponse<>(true, profile));
    }
}