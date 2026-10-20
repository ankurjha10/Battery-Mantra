package com.api.batterymantra.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
