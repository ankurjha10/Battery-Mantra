package com.api.batterymantra.dto.inventory;

import com.fasterxml.jackson.annotation.JsonSetter;
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
public class CityProductStockItem {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @JsonSetter("stockQuantity")
    public void setStockQuantity(Integer stockQuantity) {
        if (stockQuantity != null) {
            this.stock = stockQuantity;
        }
    }
}

