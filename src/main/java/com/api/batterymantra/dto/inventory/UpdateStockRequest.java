package com.api.batterymantra.dto.inventory;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity;

    private String notes;
}

