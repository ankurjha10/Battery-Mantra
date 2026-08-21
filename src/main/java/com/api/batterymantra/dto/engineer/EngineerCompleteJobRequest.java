package com.api.batterymantra.dto.engineer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EngineerCompleteJobRequest {

    @NotBlank(message = "Security OTP code is required")
    private String securityCode;

    private String installedBatterySerialNumber;
    private boolean oldBatteryCollected;
    private String oldBatteryDetails;
    private String engineerNotes;
}
