package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.MainIslandDto;
import org.example.piratelegacy.auth.dto.UserResourcesDto;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.UserProgressService;
import org.example.piratelegacy.auth.service.UserResourcesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressService userProgressService;
    private final UserResourcesService userResourcesService;

    @PostMapping("/complete/{questKey}")
    public ResponseEntity<ApiResponse<MessageResponse>> completeQuest(
            @CurrentUser User user,
            @PathVariable String questKey) {
        userProgressService.completeQuest(user, questKey);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Квест '" + questKey + "' успешно завершен.")));
    }

    /**
     * Возвращает "снимок" состояния для главного острова.
     */
    @GetMapping("/main-island")
    public ResponseEntity<ApiResponse<MainIslandDto>> getMainIsland(@CurrentUser User user) {
        MainIslandDto mainIslandData = userProgressService.getMainIslandData(user);
        return ResponseEntity.ok(new ApiResponse<>(true, mainIslandData));
    }

    /**
     * НОВЫЙ МЕТОД: Возвращает только ресурсы пользователя.
     * Полезен для динамических обновлений UI (например, после покупки).
     */
    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<UserResourcesDto>> getUserResources(@CurrentUser User user) {
        UserResourcesDto userResources = userResourcesService.getResources(user);
        return ResponseEntity.ok(new ApiResponse<>(true, userResources));
    }
}