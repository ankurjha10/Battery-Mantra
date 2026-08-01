package com.api.batterymantra.controller;

import com.api.batterymantra.dto.coupon.ApplyCouponRequest;
import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request.getCode(), request.getCartTotal()));
    }
}
