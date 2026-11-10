package com.api.batterymantra.controller.admin;

import com.api.batterymantra.dto.auth.AdminGateConfigResponse;
import com.api.batterymantra.dto.auth.UpdateGatePinRequest;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.AdminGateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSecurityController {

    private final AdminGateService adminGateService;

    @GetMapping("/gate-config")
    public ResponseEntity<AdminGateConfigResponse> getGateConfig() {
        return ResponseEntity.ok(adminGateService.getGateConfig());
    }

    @PutMapping("/gate-pin")
    public ResponseEntity<AdminGateConfigResponse> updateGatePin(
            @Valid @RequestBody UpdateGatePinRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(adminGateService.updateGatePin(userPrincipal.getUser().getUserId(), request));
    }

    @PutMapping("/users/{userId}/gate-pin")
    public ResponseEntity<AdminGateConfigResponse> updateUserGatePin(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateGatePinRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        request.setTargetUserId(userId);
        return ResponseEntity.ok(adminGateService.updateGatePin(userPrincipal.getUser().getUserId(), request));
    }
}
