package com.api.batterymantra.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequest {

    @NotBlank(message = "FCM Token cannot be blank")
    @JsonProperty("fcm_token")
    private String fcmToken;
}
