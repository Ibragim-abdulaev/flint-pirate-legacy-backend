package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.DeadPirateDto;
import org.example.piratelegacy.auth.dto.request.RevivePirateRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.ShamanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shaman")
@RequiredArgsConstructor
public class ShamanController {

    private final ShamanService shamanService;

    /** Список мёртвых пиратов с ценами воскрешения */
    @GetMapping("/dead")
    public ResponseEntity<ApiResponse<List<DeadPirateDto>>> getDeadPirates(@CurrentUser User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, shamanService.getDeadPirates(user)));
    }

    /** Воскресить пирата. Body: { "paymentType": "GOLD" или "CRYSTALS" } */
    @PostMapping("/revive/{unitId}")
    public ResponseEntity<ApiResponse<Void>> revivePirate(
            @CurrentUser User user,
            @PathVariable Long unitId,
            @RequestBody RevivePirateRequest request) {
        shamanService.revivePirate(user, unitId, request.getPaymentType());
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }

    /** Моментально воскресить героя за 300 кристаллов */
    @PostMapping("/hero/revive")
    public ResponseEntity<ApiResponse<Void>> reviveHeroInstant(@CurrentUser User user) {
        shamanService.reviveHeroInstant(user);
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }

    /** Проверить восстановился ли герой по таймеру (вызывать при входе в игру) */
    @PostMapping("/hero/check-recovery")
    public ResponseEntity<ApiResponse<Void>> checkHeroRecovery(@CurrentUser User user) {
        shamanService.checkHeroRecovery(user);
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }
}