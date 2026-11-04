package com.api.batterymantra.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final int availableStock;

    public InsufficientStockException(String message, UUID productId, int availableStock) {
        super(message);
        this.productId = productId;
        this.availableStock = availableStock;
    }
}

