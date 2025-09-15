package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.QuestJsonDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.service.QuestService;
import org.example.piratelegacy.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quest")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    private final UserService userService;

    @GetMapping("/current")
    public ResponseEntity<QuestJsonDto> getCurrentQuest(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            QuestJsonDto quest = questService.getCurrentQuest(user);
            return ResponseEntity.ok(quest);
        } catch (RuntimeException e) {
            // Если нет квестов, возвращаем 404
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/complete/{questId}")
    public ResponseEntity<QuestJsonDto> completeQuest(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long questId) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            QuestJsonDto nextQuest = questService.completeQuest(user, questId);
            return ResponseEntity.ok(nextQuest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/has-active")
    public ResponseEntity<Boolean> hasActiveQuest(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasActive = questService.hasActiveQuest(user);
        return ResponseEntity.ok(hasActive);
    }
}