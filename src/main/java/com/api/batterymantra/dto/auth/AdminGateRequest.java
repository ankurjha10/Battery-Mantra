package com.api.batterymantra.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGateRequest {

    @NotBlank(message = "PIN is required")
    private String pin;
}

