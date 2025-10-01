package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.PopupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/popup")
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    private PopupService.PopupType getPopupTypeFromString(String popupTypeStr) {
        try {
            return PopupService.PopupType.valueOf(popupTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid popup type: " + popupTypeStr, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/should-show/{popupType}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> shouldShowPopup(
            @CurrentUser User user,
            @PathVariable("popupType") String popupTypeStr) {

        PopupService.PopupType popup = getPopupTypeFromString(popupTypeStr);
        boolean shouldShow = popupService.shouldShowPopup(user, popup);
        return ResponseEntity.ok(new ApiResponse<>(true, Map.of("shouldShow", shouldShow)));
    }

    @PostMapping("/mark-shown/{popupType}")
    public ResponseEntity<ApiResponse<MessageResponse>> markPopupShown(
            @CurrentUser User user,
            @PathVariable("popupType") String popupTypeStr) {

        PopupService.PopupType popup = getPopupTypeFromString(popupTypeStr);
        popupService.markPopupAsShown(user, popup);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("success")));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPopupStatuses(@CurrentUser User user) {
        Map<String, Boolean> statuses = Arrays.stream(PopupService.PopupType.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        pt -> popupService.shouldShowPopup(user, pt)
                ));
        return ResponseEntity.ok(new ApiResponse<>(true, statuses));
    }
}