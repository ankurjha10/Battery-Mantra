package com.api.batterymantra.dto.inventory;

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
public class PartnerInventoryItemResponse {

    private UUID productId;
    private String productName;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private Integer productStock;
    private String brandName;
    private String capacity;
    private String productCategory;
    private String productImage;
}

