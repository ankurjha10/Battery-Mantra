package com.api.batterymantra.dto.cart;

import com.api.batterymantra.dto.product.ProductListResponse;
import lombok.Data;

import java.util.UUID;

@Data
public class CartItemResponse {
    public UUID cartItemId;
    public ProductListResponse product;
    public int quantity;
    public boolean exchangeOldBattery;
}

