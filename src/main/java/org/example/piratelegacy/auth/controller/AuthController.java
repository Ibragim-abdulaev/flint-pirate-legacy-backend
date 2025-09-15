package org.example.piratelegacy.auth.controller;

import jakarta.validation.Valid;
import lombok.Data;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.JwtService;
import org.example.piratelegacy.auth.service.UserResourcesService;
import org.example.piratelegacy.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserResourcesService userResourcesService;
    private final JwtService jwtService;
    private final CharacterSelectionService characterSelectionService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          UserResourcesService userResourcesService,
                          JwtService jwtService,
                          CharacterSelectionService characterSelectionService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userResourcesService = userResourcesService;
        this.jwtService = jwtService;
        this.characterSelectionService = characterSelectionService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        userResourcesService.createInitialResources(user);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, false, true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(userDetails);
        boolean hasCharacter = characterSelectionService.userHasCharacter(user);

        return ResponseEntity.ok(new AuthResponse(token, hasCharacter, false));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasCharacter = characterSelectionService.userHasCharacter(user);

        return ResponseEntity.ok(new UserProfileResponse(user, hasCharacter));
    }

    @GetMapping("/has-character")
    public ResponseEntity<Map<String, Boolean>> hasCharacter(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasCharacter = characterSelectionService.userHasCharacter(user);
        return ResponseEntity.ok(Map.of("hasCharacter", hasCharacter));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasCharacter = characterSelectionService.userHasCharacter(user);
        return ResponseEntity.ok(new UserProfileResponse(user, hasCharacter));
    }

    // ------------------- Новый Logout -------------------
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // DTO классы (без изменений)
    @Data
    private static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    private static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    private static class AuthResponse {
        private final String token;
        private final boolean hasCharacter;
        private final boolean isNewUser;

        public AuthResponse(String token, boolean hasCharacter, boolean isNewUser) {
            this.token = token;
            this.hasCharacter = hasCharacter;
            this.isNewUser = isNewUser;
        }
    }

    @Data
    private static class UserProfileResponse {
        private final Long id;
        private final String username;
        private final String email;
        private final boolean hasCharacter;

        public UserProfileResponse(User user, boolean hasCharacter) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.hasCharacter = hasCharacter;
        }
    }
}