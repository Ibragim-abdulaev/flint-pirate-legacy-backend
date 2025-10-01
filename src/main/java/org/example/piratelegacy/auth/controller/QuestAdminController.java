package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.service.QuestInitializationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/quests")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.admin.enabled", havingValue = "true")
public class QuestAdminController {

    private final QuestInitializationService questInitializationService;

    @PostMapping("/reload")
    public ResponseEntity<ApiResponse<MessageResponse>> reloadQuests() {
        try {
            questInitializationService.reloadQuestsFromJson();
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    new MessageResponse("Quests reloaded successfully")
            ));
        } catch (Exception e) {
            throw new ApiException("Failed to reload quests: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}