package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.JournalDto;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public ResponseEntity<ApiResponse<JournalDto>> getJournal(@CurrentUser User user) {
        JournalDto journalData = journalService.getJournalForUser(user);
        return ResponseEntity.ok(new ApiResponse<>(true, journalData));
    }
}