package org.example.piratelegacy.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.request.LoginRequest;
import org.example.piratelegacy.auth.dto.request.RegisterRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.dto.response.AuthResponse;
import org.example.piratelegacy.auth.dto.response.MessageResponse;
import org.example.piratelegacy.auth.dto.response.UserProfileResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.CharacterSelectionService;
import org.example.piratelegacy.auth.service.PopupService;
import org.example.piratelegacy.auth.service.UserResourcesService;
import org.example.piratelegacy.auth.service.UserService;
import org.example.piratelegacy.auth.service.jwt.JwtBlacklistService;
import org.example.piratelegacy.auth.service.jwt.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    private final PopupService popupService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        userResourcesService.createInitialResources(user.getId());
        popupService.initializePopupsForUser(user);

        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse(token, false, true);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Long userId = Long.valueOf(userDetails.getUsername());

        User user = userService.findUserById(userId);

        String token = jwtService.generateToken(userDetails);
        boolean hasCharacter = characterSelectionService.userHasCharacter(user);
        AuthResponse response = new AuthResponse(token, hasCharacter, false);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@CurrentUser User user) {
        boolean hasCharacter = characterSelectionService.userHasCharacter(user);
        UserProfileResponse response = new UserProfileResponse(user, hasCharacter);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.addToBlacklist(token);
            log.info("Token for user has been blacklisted.");
        }
        SecurityContextHolder.clearContext();
        MessageResponse response = new MessageResponse("Logged out successfully");
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }
}