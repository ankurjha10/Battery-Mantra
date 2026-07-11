package com.api.batterymantra.dto.callback;

import com.api.batterymantra.entity.enums.CallbackStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CallbackResponse {
    private Long callbackId;
    private String mobileNumber;
    private CallbackStatus status;
    private LocalDateTime createdAt;
}
