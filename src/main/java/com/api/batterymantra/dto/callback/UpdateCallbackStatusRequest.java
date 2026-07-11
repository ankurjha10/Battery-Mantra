package com.api.batterymantra.dto.callback;

import com.api.batterymantra.entity.enums.CallbackStatus;
import lombok.Data;

@Data
public class UpdateCallbackStatusRequest {
    private CallbackStatus status;
}
