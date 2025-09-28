package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.QuestJsonDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quest")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    @GetMapping("/current")
    public ResponseEntity<QuestJsonDto> getCurrentQuest(@CurrentUser User user) {
        try {
            QuestJsonDto quest = questService.getCurrentQuest(user);
            return ResponseEntity.ok(quest);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/complete/{questId}")
    public ResponseEntity<QuestJsonDto> completeQuest(@CurrentUser User user,
                                                      @PathVariable Long questId) {
        try {
            QuestJsonDto nextQuest = questService.completeQuest(user, questId);
            return ResponseEntity.ok(nextQuest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/has-active")
    public ResponseEntity<Boolean> hasActiveQuest(@CurrentUser User user) {
        boolean hasActive = questService.hasActiveQuest(user);
        return ResponseEntity.ok(hasActive);
    }
}