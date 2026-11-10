package com.api.batterymantra.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGatePinRequest {

    private UUID targetUserId;

    private String targetRole;

    @NotBlank(message = "newPin is required")
    @Size(min = 4, max = 32, message = "newPin must be between 4 and 32 characters")
    private String newPin;

    @NotBlank(message = "currentPassword is required")
    private String currentPassword;
}
