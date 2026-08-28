package com.api.batterymantra.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BulkProductUpdateRequest {
    private UUID productId;
    private BigDecimal productPrice;
    private BigDecimal originalPrice;
    private BigDecimal exchangeDiscount;
    private String highlights;
}
