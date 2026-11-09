package com.api.batterymantra.dto.product;

import com.api.batterymantra.entity.enums.PricingRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityPricingRequestResponse {

    private UUID requestId;
    private UUID productId;
    private String productName;
    private UUID cityId;
    private String cityName;
    private UUID partnerId;
    private String partnerBusinessName;
    private BigDecimal price;
    private BigDecimal exchangeDiscount;
    private Integer stock;
    private String reason;
    private PricingRequestStatus status;
    private String rejectionReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
