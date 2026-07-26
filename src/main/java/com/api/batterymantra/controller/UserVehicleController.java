package com.api.batterymantra.controller;

import com.api.batterymantra.dto.vehicle.UserVehicleRequest;
import com.api.batterymantra.dto.vehicle.UserVehicleResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.UserVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/vehicles")
@RequiredArgsConstructor
public class UserVehicleController {

    private final UserVehicleService userVehicleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserVehicleResponse>> getUserVehicles(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<UserVehicleResponse> vehicles = userVehicleService.getUserVehicles(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserVehicleResponse> addUserVehicle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserVehicleRequest request) {
        UserVehicleResponse response = userVehicleService.addUserVehicle(userPrincipal.getUser().getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteUserVehicle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID vehicleId) {
        userVehicleService.deleteUserVehicle(userPrincipal.getUser().getUserId(), vehicleId);
        return ResponseEntity.ok().build();
    }
}
