package com.api.batterymantra.controller;

import com.api.batterymantra.dto.coupon.ApplyCouponRequest;
import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.dto.coupon.CouponResponse;
import com.api.batterymantra.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/active")
    public ResponseEntity<List<CouponResponse>> getActivePublicCoupons() {
        return ResponseEntity.ok(couponService.getActivePublicCoupons());
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }
}
