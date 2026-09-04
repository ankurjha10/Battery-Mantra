package com.api.batterymantra.dto.product;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductReorderRequest {
    private UUID productId;
    private Integer orderValue;
    private String orderContext; // "GLOBAL", "CATEGORY", or "BRAND"
}
