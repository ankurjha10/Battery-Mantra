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
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User with ID {} not found. Cannot send notification.", userId);
                return;
            }

            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .build();
            notificationRepository.save(notification);

            String fcmToken = user.getFcmToken();
            if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(fcmToken)
                        .putData("title", title)
                        .putData("body", message)
                        .putData("type", "custom_ui");
                if (dataPayload != null) {
                    messageBuilder.putAllData(dataPayload);
                }
                String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
                log.info("Successfully sent message to user {}: {}", userId, response);
            } else {
                log.info("User {} has no FCM token registered.", userId);
            }
        } catch (Exception e) {
            log.error("sendPushNotification totally fail for user {} — swallow, don't break caller tx", userId, e);
        }
    }
}
