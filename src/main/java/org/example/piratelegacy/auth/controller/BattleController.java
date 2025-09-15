package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.BattleLocationDto;
import org.example.piratelegacy.auth.dto.BattleLogResultDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.dto.BattleResultDto;
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
    public ResponseEntity<BattleLocationDto> getFirstQuestLocation() {
        BattleLocationDto location = battleLocationService.generateFirstQuestLocation();
        return ResponseEntity.ok(location);
    }

    @PostMapping("/first-quest-fight")
    public ResponseEntity<BattleLogResultDto> fight(@RequestBody List<BattlePirateDto> pirates) {
        return ResponseEntity.ok(battleService.fight(pirates));
    }
}
