package com.api.batterymantra.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemSnapshot {

    private UUID productId;
    private int quantity;
    private BigDecimal priceAtPurchase;
    private boolean exchangeOldBattery;
    private BigDecimal exchangeDiscount;
}
