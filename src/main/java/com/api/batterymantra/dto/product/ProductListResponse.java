package com.api.batterymantra.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductListResponse {
    private UUID productId;
    private String productName;
    private String brandName;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private String productImage;
    private List<String> additionalImages;
    private String productCategory;
    private String capacity;
}
