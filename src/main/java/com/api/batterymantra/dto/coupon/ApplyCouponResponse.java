package com.api.batterymantra.dto.coupon;

import com.api.batterymantra.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponse {
    private String couponId;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private Double discountAmount;
    private Boolean isValid;
    private String message;
    private Double finalTotal;
}
