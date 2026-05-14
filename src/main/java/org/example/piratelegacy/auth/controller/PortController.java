package org.example.piratelegacy.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.ShipTypeDto;
import org.example.piratelegacy.auth.dto.UserShipDto;
import org.example.piratelegacy.auth.dto.request.AssignUnitToShipRequest;
import org.example.piratelegacy.auth.dto.request.BuyShipRequest;
import org.example.piratelegacy.auth.dto.request.SetShipModeRequest;
import org.example.piratelegacy.auth.dto.response.ApiResponse;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.security.annotation.CurrentUser;
import org.example.piratelegacy.auth.service.PortService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/port")
@RequiredArgsConstructor
public class PortController {

    private final PortService portService;

    @GetMapping("/ships/available")
    public ResponseEntity<ApiResponse<List<ShipTypeDto>>> getAvailableShipTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.getAvailableShipTypes()));
    }

    @GetMapping("/ships")
    public ResponseEntity<ApiResponse<List<UserShipDto>>> getUserShips(@CurrentUser User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.getUserShips(user)));
    }

    @PostMapping("/ships/buy")
    public ResponseEntity<ApiResponse<UserShipDto>> buyShip(
            @CurrentUser User user,
            @RequestBody BuyShipRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.buyShip(user, request)));
    }

    @PutMapping("/ships/{shipId}/mode")
    public ResponseEntity<ApiResponse<UserShipDto>> setShipMode(
            @CurrentUser User user,
            @PathVariable Long shipId,
            @RequestBody SetShipModeRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.setShipMode(user, shipId, request)));
    }

    @PostMapping("/ships/{shipId}/crew")
    public ResponseEntity<ApiResponse<UserShipDto>> assignUnitToShip(
            @CurrentUser User user,
            @PathVariable Long shipId,
            @RequestBody AssignUnitToShipRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.assignUnitToShip(user, shipId, request)));
    }

    @DeleteMapping("/ships/{shipId}/crew/{unitId}")
    public ResponseEntity<ApiResponse<Void>> removeUnitFromShip(
            @CurrentUser User user,
            @PathVariable Long shipId,
            @PathVariable Long unitId) {
        portService.removeUnitFromShip(user, shipId, unitId);
        return ResponseEntity.ok(new ApiResponse<>(true, null));
    }

    @PostMapping("/ships/{shipId}/upgrade")
    public ResponseEntity<ApiResponse<UserShipDto>> upgradeShip(
            @CurrentUser User user,
            @PathVariable Long shipId) {
        return ResponseEntity.ok(new ApiResponse<>(true, portService.upgradeShip(user, shipId)));
    }
}