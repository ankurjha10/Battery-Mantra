package com.api.batterymantra.dto.engineer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EngineerCompleteJobRequest {

    @NotBlank(message = "OTP is required")
    private String otp;

    private String serialNumber;

    private boolean oldBatteryCollected;

    private String paymentMode;
}
