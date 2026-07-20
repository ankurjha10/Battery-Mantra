package com.api.batterymantra.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BulkPricingRequest {
    private UUID categoryId;
    private UUID brandId;
    private UUID cityId;
    private BigDecimal percentage;
}
