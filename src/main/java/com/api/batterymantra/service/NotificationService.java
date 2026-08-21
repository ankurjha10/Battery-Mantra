package com.api.batterymantra.service;

import com.api.batterymantra.entity.Notification;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.repository.NotificationRepository;
import com.api.batterymantra.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public void sendPushNotification(UUID userId, String title, String message, Map<String, String> dataPayload) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User with ID {} not found. Cannot send notification.", userId);
            return;
        }

        // Save Notification to Database
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);

        // Send Push Notification if FCM token exists
        String fcmToken = user.getFcmToken();
        if (fcmToken != null && !fcmToken.trim().isEmpty()) {
            try {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(fcmToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(message)
                                .build());

                if (dataPayload != null) {
                    messageBuilder.putAllData(dataPayload);
                }

                Message fcmMessage = messageBuilder.build();
                String response = FirebaseMessaging.getInstance().send(fcmMessage);
                log.info("Successfully sent message to user {}: {}", userId, response);
            } catch (Exception e) {
                log.error("Failed to send Firebase push notification to user {}", userId, e);
            }
        } else {
            log.info("User {} has no FCM token registered.", userId);
        }
    }
}
