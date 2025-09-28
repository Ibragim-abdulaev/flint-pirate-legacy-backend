package org.example.piratelegacy.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.dto.response.MessageResponseDto;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.jwt.JwtBlacklistService;
import org.example.piratelegacy.auth.service.jwt.JwtService;
import org.example.piratelegacy.auth.service.UserResourcesService;
import org.example.piratelegacy.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserResourcesService userResourcesService;
    private final JwtService jwtService;
    private final CharacterSelectionService characterSelectionService;
    private final JwtBlacklistService jwtBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        userResourcesService.createInitialResources(user);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, false, true));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userService.findUserById(Long.valueOf(userDetails.getUsername()));

        String token = jwtService.generateToken(userDetails);
        boolean hasCharacter = characterSelectionService.userHasCharacter(user);

        return ResponseEntity.ok(new AuthResponse(token, hasCharacter, false));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@CurrentUser User user) {
        boolean hasCharacter = characterSelectionService.userHasCharacter(user);
        return ResponseEntity.ok(new UserProfileResponse(user, hasCharacter));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.addToBlacklist(token);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponseDto("Logged out successfully"));
    }

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