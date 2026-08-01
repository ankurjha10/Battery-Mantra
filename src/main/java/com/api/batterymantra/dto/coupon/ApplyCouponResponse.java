package com.api.batterymantra.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponse {
    private Boolean isValid;
    private Double discountAmount;
    private String message;
    private Double finalTotal;
}
