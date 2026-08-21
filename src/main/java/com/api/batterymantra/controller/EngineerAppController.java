package com.api.batterymantra.controller;

import com.api.batterymantra.dto.engineer.EngineerCompleteJobRequest;
import com.api.batterymantra.dto.engineer.EngineerFailJobRequest;
import com.api.batterymantra.dto.engineer.UpdateDutyStatusRequest;
import com.api.batterymantra.dto.engineer.UpdateFcmTokenRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.EngineerService;
import com.api.batterymantra.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/engineer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ENGINEER')")
public class EngineerAppController {

    private final EngineerService engineerService;
    private final OrderService orderService;

    @GetMapping("/profile")
    public ResponseEntity<EngineerResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                engineerService.getEngineerProfileByUserId(userPrincipal.getUser().getUserId()));
    }

    @PatchMapping("/duty-status")
    public ResponseEntity<EngineerResponse> updateDutyStatus(
            @Valid @RequestBody UpdateDutyStatusRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                engineerService.updateDutyStatus(userPrincipal.getUser().getUserId(), request.getDutyStatus()));
    }

    @PatchMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @Valid @RequestBody UpdateFcmTokenRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        engineerService.updateFcmToken(userPrincipal.getUser().getUserId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAssignedOrders(
            @RequestParam(defaultValue = "ACTIVE") String filter,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                orderService.getEngineerOrders(userPrincipal.getUser().getUserId(), filter));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                orderService.getEngineerOrderById(orderId, userPrincipal.getUser().getUserId()));
    }

    @PatchMapping("/orders/{orderId}/start")
    public ResponseEntity<OrderResponse> startJob(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                orderService.startEngineerJob(orderId, userPrincipal.getUser().getUserId()));
    }

    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<OrderResponse> completeJob(
            @PathVariable UUID orderId,
            @Valid @RequestBody EngineerCompleteJobRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                orderService.completeEngineerJob(orderId, userPrincipal.getUser().getUserId(), request));
    }

    @PostMapping("/orders/{orderId}/fail")
    public ResponseEntity<OrderResponse> failJob(
            @PathVariable UUID orderId,
            @Valid @RequestBody EngineerFailJobRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(
                orderService.failEngineerJob(orderId, userPrincipal.getUser().getUserId(), request));
    }
}
