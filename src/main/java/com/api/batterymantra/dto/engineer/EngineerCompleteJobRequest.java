package com.api.batterymantra.dto.engineer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EngineerCompleteJobRequest {

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotBlank(message = "Serial Number is required")
    private String serialNumber;

    private boolean oldBatteryCollected;

    private String paymentMode;
}
