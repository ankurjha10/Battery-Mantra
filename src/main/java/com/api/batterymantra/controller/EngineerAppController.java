package com.api.batterymantra.controller;

import com.api.batterymantra.dto.engineer.EngineerCompleteJobRequest;
import com.api.batterymantra.dto.engineer.EngineerFailJobRequest;
import com.api.batterymantra.dto.engineer.UpdateFcmTokenRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.EngineerService;
import com.api.batterymantra.service.OrderService;
import com.api.batterymantra.service.EngineerAttendanceService;
import com.api.batterymantra.dto.engineer.CreateLeaveRequest;
import com.api.batterymantra.entity.EngineerAttendance;
import com.api.batterymantra.entity.LeaveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.api.batterymantra.dto.admin.UserResponse;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.DutyStatus;

@RestController
@RequestMapping("/api/engineer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ENGINEER')")
public class EngineerAppController {

    private final EngineerService engineerService;
    private final OrderService orderService;
    private final EngineerAttendanceService attendanceService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        EngineerResponse engineerResponse = engineerService.getEngineerProfileByUserId(user.getUserId());

        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .name(engineerResponse.getFirstName() != null
                        ? engineerResponse.getFirstName() + " " + engineerResponse.getLastName()
                        : user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/duty-status")
    public ResponseEntity<UserResponse> updateDutyStatus(
            @RequestParam boolean isOnDuty,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DutyStatus status = isOnDuty
                ? DutyStatus.ON_DUTY
                : DutyStatus.OFF_DUTY;

        EngineerResponse engineerResponse = engineerService.updateDutyStatus(userPrincipal.getUser().getUserId(),
                status);
        User user = userPrincipal.getUser();

        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .name(engineerResponse.getFirstName() != null
                        ? engineerResponse.getFirstName() + " " + engineerResponse.getLastName()
                        : user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(isOnDuty)
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
        return ResponseEntity.ok(response);
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

    @PostMapping("/orders/{orderId}/log-call")
    public ResponseEntity<Void> logCall(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        orderService.logCall(orderId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<EngineerAttendance> checkIn(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(attendanceService.checkIn(userPrincipal.getUser().getUserId()));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<EngineerAttendance> checkOut(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(attendanceService.checkOut(userPrincipal.getUser().getUserId()));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<EngineerAttendance>> getMyAttendance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(userPrincipal.getUser().getUserId()));
    }

    @PostMapping("/leave-requests")
    public ResponseEntity<LeaveRequest> applyForLeave(
            @Valid @RequestBody CreateLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(attendanceService.applyForLeave(userPrincipal.getUser().getUserId(), request));
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<List<LeaveRequest>> getMyLeaves(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(attendanceService.getMyLeaves(userPrincipal.getUser().getUserId()));
    }
}
