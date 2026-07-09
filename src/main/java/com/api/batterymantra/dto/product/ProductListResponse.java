package com.api.batterymantra.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductListResponse {
    private UUID productId;
    private String productName;
    private String brandName;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private String productImage;
    private String productCategory;
}
