package com.api.batterymantra.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStockUpdateRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity;

    private String notes;
}

