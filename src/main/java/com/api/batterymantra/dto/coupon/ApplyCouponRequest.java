package com.api.batterymantra.dto.coupon;

import lombok.Data;

@Data
public class ApplyCouponRequest {
    private String code;
    private Double cartTotal;
}
