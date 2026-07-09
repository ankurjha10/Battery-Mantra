package com.api.batterymantra.dto.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CartResponse {
    public UUID cartId;
    public UUID userId;
    public List<CartItemResponse> cartItems;
    public BigDecimal subTotal;
    public BigDecimal exchangeDiscount;
    public BigDecimal totalAmount;
}

