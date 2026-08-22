package com.api.batterymantra.controller;

import com.api.batterymantra.dto.notification.NotificationResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<NotificationResponse> notifications = notificationRepository
                .findByUserUserIdOrderByCreatedAtDesc(userPrincipal.getUser().getUserId())
                .stream()
                .map(notification -> NotificationResponse.builder()
                        .id(notification.getId())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(notifications);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationRepository.deleteAllByUserUserId(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok().build();
    }
}