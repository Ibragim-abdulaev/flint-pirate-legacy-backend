package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.*;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.exception.ApiException;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.BattleLocationService;
import org.example.piratelegacy.auth.service.BattleRewardService;
import org.example.piratelegacy.auth.service.BattleService;
import org.example.piratelegacy.auth.service.QuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleController {

    private final BattleLocationService battleLocationService;
    private final BattleService battleService;
    private final QuestService questService;
    private final BattleRewardService battleRewardService;

    @GetMapping("/location/quest/{questKey}")
    public ResponseEntity<ApiResponse<BattleLocationDto>> getQuestBattleLocation(
            @CurrentUser User user,
            @PathVariable String questKey) {

        Quest quest = questService.getQuestByKey(questKey);

        if (quest.getBattleLocationId() == null || quest.getBattleLocationId().isEmpty()) {
            throw new ApiException("This quest does not have a battle location.", HttpStatus.BAD_REQUEST);
        }

        BattleLocationDto location = battleLocationService.getOrCreateBattleLocation(
                user.getId(), quest.getBattleLocationId());
        return ResponseEntity.ok(new ApiResponse<>(true, location));
    }

    @PostMapping("/placement/quest/{questKey}/move")
    public ResponseEntity<ApiResponse<List<BattlePirateDto>>> movePirate(
            @CurrentUser User user,
            @RequestBody PirateMoveRequestDto moveRequest) {
        List<BattlePirateDto> updatedPirates = battleLocationService.movePirateDuringPlacement(user.getId(), moveRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, updatedPirates));
    }

    @PostMapping("/fight/{questKey}")
    public ResponseEntity<ApiResponse<BattleResultDto>> fight(
            @CurrentUser User user,
            @PathVariable String questKey,
            @RequestBody List<BattlePirateDto> pirates) {

        BattleResultDto result = battleService.fight(questKey, pirates);
        battleLocationService.endBattle(user.getId());

        // Начисляем опыт выжившим, помечаем погибших, начисляем опыт острову
        battleRewardService.processBattleRewards(user, result, pirates);

        return ResponseEntity.ok(new ApiResponse<>(true, result));
    }
}