package com.api.batterymantra.dto.cart;

import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartRequest {
    public UUID productId;
    public int quantity;
    public boolean exchangeOldBattery;
}
