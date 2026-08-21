package com.api.batterymantra.dto.engineer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EngineerFailJobRequest {

    @NotBlank(message = "Failure reason is required")
    private String failureReason;

    private String notes;
}
