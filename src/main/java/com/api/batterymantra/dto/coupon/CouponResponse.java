package com.api.batterymantra.dto.coupon;

import com.api.batterymantra.entity.enums.DiscountType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CouponResponse {
    private UUID couponId;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private Double maxDiscountAmount;
    private Double minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
