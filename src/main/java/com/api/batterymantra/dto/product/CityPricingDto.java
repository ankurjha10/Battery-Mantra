package com.api.batterymantra.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityPricingDto {
    private UUID cityId;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be positive or zero")
    private BigDecimal price;

    @Min(value = 0, message = "Exchange discount cannot be negative")
    private BigDecimal exchangeDiscount;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;
}
