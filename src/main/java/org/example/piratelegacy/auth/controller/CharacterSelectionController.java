package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.CharacterInfo;
import org.example.piratelegacy.auth.dto.CharacterSelectionRequest;
import org.example.piratelegacy.auth.entity.enums.CharacterType;
import org.example.piratelegacy.auth.entity.Hero;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/character")
@RequiredArgsConstructor
public class CharacterSelectionController {

    private final CharacterSelectionService characterSelectionService;
    private final QuestService questService;

    private static final Set<CharacterType> STARTER_CHARACTERS = Set.of(
            CharacterType.BARBARIAN,
            CharacterType.ARCHER
    );

    @GetMapping("/options")
    public ResponseEntity<List<CharacterInfo>> getCharacterOptions() {
        List<CharacterInfo> options = STARTER_CHARACTERS.stream()
                .map(this::toCharacterInfo)
                .toList();

        return ResponseEntity.ok(options);
    }

    @PostMapping("/select")
    public ResponseEntity<Map<String, Object>> selectCharacter(
            @CurrentUser User user,
            @RequestBody CharacterSelectionRequest request) {

        try {
            Hero hero = characterSelectionService.selectCharacter(user, request);
            questService.initializeFirstQuest(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Персонаж успешно создан",
                    "hero", hero
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<Hero> getCurrentCharacter(@CurrentUser User user) {
        try {
            Hero hero = characterSelectionService.getUserCharacter(user);
            return ResponseEntity.ok(hero);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private CharacterInfo toCharacterInfo(CharacterType characterType) {
        return new CharacterInfo(
                characterType.name(),
                characterType.getDisplayName(),
                characterType.getCombatClass().name(),
                characterType.getCombatClass().getDisplayName(),
                characterType.getBaseHp(),
                characterType.getMinAttack(),
                characterType.getMaxAttack(),
                characterType.getBaseArmor(),
                characterType.getSpecialAbility(),
                characterType.hasSpecialAbility()
        );
    }
}