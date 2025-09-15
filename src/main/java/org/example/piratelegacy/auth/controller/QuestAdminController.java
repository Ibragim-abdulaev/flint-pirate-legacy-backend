package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.service.QuestInitializationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/quests")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.admin.enabled", havingValue = "true", matchIfMissing = false)
public class QuestAdminController {

    private final QuestInitializationService questInitializationService;

    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reloadQuests() {
        try {
            questInitializationService.reloadQuestsFromJson();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Quests reloaded successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to reload quests: " + e.getMessage()
            ));
        }
    }
}
