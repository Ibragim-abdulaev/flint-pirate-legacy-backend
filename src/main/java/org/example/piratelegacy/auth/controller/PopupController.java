package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.PopupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/popup")
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    @GetMapping("/should-show/{popupType}")
    public ResponseEntity<Map<String, Boolean>> shouldShowPopup(
            @CurrentUser User user,
            @PathVariable("popupType") String popupType) {

        try {
            PopupService.PopupType popup = PopupService.PopupType.valueOf(popupType.toUpperCase());
            boolean shouldShow = popupService.shouldShowPopup(user, popup);
            return ResponseEntity.ok(Map.of("shouldShow", shouldShow));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("shouldShow", false));
        }
    }

    @PostMapping("/mark-shown/{popupType}")
    public ResponseEntity<Map<String, String>> markPopupShown(
            @CurrentUser User user,
            @PathVariable("popupType") String popupType) {

        try {
            PopupService.PopupType popup = PopupService.PopupType.valueOf(popupType.toUpperCase());
            popupService.markPopupAsShown(user, popup);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid popup type"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getPopupStatuses(@CurrentUser User user) {
        Map<String, Boolean> statuses = Map.of(
                "welcome", popupService.shouldShowPopup(user, PopupService.PopupType.WELCOME),
                "firstQuest", popupService.shouldShowPopup(user, PopupService.PopupType.FIRST_QUEST)
        );

        return ResponseEntity.ok(statuses);
    }
}