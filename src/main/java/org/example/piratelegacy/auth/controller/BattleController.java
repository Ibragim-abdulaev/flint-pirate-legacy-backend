package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.BattleLocationDto;
import org.example.piratelegacy.auth.dto.BattleLogResultDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.dto.PirateMoveRequestDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.BattleLocationService;
import org.example.piratelegacy.auth.service.BattleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleController {

    private final BattleLocationService battleLocationService;
    private final BattleService battleService;

    @GetMapping("/first-quest-location")
    public ResponseEntity<BattleLocationDto> getFirstQuestLocation(@CurrentUser User user) {
        BattleLocationDto location = battleLocationService.getFirstQuestLocation(user.getId());
        return ResponseEntity.ok(location);
    }

    @PostMapping("/placement/move")
    public ResponseEntity<List<BattlePirateDto>> movePirate(@CurrentUser User user, @RequestBody PirateMoveRequestDto moveRequest) {
        List<BattlePirateDto> updatedPirates = battleLocationService.movePirateDuringPlacement(user.getId(), moveRequest);
        return ResponseEntity.ok(updatedPirates);
    }

    @PostMapping("/first-quest-fight")
    public ResponseEntity<BattleLogResultDto> fight(@RequestBody List<BattlePirateDto> pirates) {
        return ResponseEntity.ok(battleService.fight(pirates));
    }
}