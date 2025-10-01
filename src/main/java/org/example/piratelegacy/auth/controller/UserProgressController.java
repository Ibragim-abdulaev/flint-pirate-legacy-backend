package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.UserProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressService userProgressService;

    @PostMapping("/complete/{questKey}")
    public ResponseEntity<ApiResponse<MessageResponse>> completeQuest(
            @CurrentUser User user,
            @PathVariable String questKey) {
        userProgressService.completeQuest(user, questKey);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Квест '" + questKey + "' успешно завершен.")));
    }
}