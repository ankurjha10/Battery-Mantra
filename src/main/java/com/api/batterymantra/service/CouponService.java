package com.api.batterymantra.service;

import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.dto.coupon.CouponRequest;
import com.api.batterymantra.dto.coupon.CouponResponse;
import com.api.batterymantra.entity.Coupon;
import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponResponse createCoupon(CouponRequest request) {
        Coupon coupon = new Coupon();
        mapRequestToEntity(request, coupon);
        coupon = couponRepository.save(coupon);
        return mapEntityToResponse(coupon);
    }

    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found with id: " + id));
        mapRequestToEntity(request, coupon);
        coupon = couponRepository.save(coupon);
        return mapEntityToResponse(coupon);
    }

    public void deleteCoupon(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new RuntimeException("Coupon not found with id: " + id);
        }
        try {
            couponRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Cannot delete coupon because it has been used in orders. Please deactivate it instead.");
        }
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    public CouponResponse getCouponById(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found with id: " + id));
        return mapEntityToResponse(coupon);
    }

    public ApplyCouponResponse applyCoupon(String code, Double cartTotal) {
        Optional<Coupon> optionalCoupon = couponRepository.findByCode(code);
        
        if (optionalCoupon.isEmpty()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Invalid coupon code")
                    .finalTotal(cartTotal)
                    .build();
        }
        
        Coupon coupon = optionalCoupon.get();
        
        if (!coupon.getIsActive()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon is inactive")
                    .finalTotal(cartTotal)
                    .build();
        }
        
        LocalDateTime now = LocalDateTime.now();
        if ((coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) || 
            (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate()))) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon has expired or is not yet active")
                    .finalTotal(cartTotal)
                    .build();
        }
        
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon usage limit exceeded")
                    .finalTotal(cartTotal)
                    .build();
        }
        
        if (cartTotal < coupon.getMinOrderValue()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Cart total must be at least " + coupon.getMinOrderValue() + " to use this coupon")
                    .finalTotal(cartTotal)
                    .build();
        }
        
        Double discountAmount = 0.0;
        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discountAmount = Math.min(coupon.getDiscountValue(), cartTotal);
        } else if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = (cartTotal * coupon.getDiscountValue()) / 100;
            if (coupon.getMaxDiscountAmount() != null) {
                discountAmount = Math.min(discountAmount, coupon.getMaxDiscountAmount());
            }
        }
        
        return ApplyCouponResponse.builder()
                .isValid(true)
                .discountAmount(discountAmount)
                .message("Coupon applied successfully")
                .finalTotal(cartTotal - discountAmount)
                .build();
    }

    public void incrementCouponUsage(String code) {
        couponRepository.findByCode(code).ifPresent(coupon -> {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        });
    }

    private void mapRequestToEntity(CouponRequest request, Coupon entity) {
        entity.setCode(request.getCode());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : 0.0);
        
        if (request.getStartDate() != null) {
            entity.setStartDate(request.getStartDate());
        } else {
            entity.setStartDate(LocalDateTime.of(2000, 1, 1, 0, 0));
        }

        if (request.getExpiryDate() != null) {
            entity.setExpiryDate(request.getExpiryDate());
        } else {
            entity.setExpiryDate(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
        }

        entity.setUsageLimit(request.getUsageLimit());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    private CouponResponse mapEntityToResponse(Coupon entity) {
        CouponResponse response = new CouponResponse();
        response.setCouponId(entity.getCouponId());
        response.setCode(entity.getCode());
        response.setDiscountType(entity.getDiscountType());
        response.setDiscountValue(entity.getDiscountValue());
        response.setMaxDiscountAmount(entity.getMaxDiscountAmount());
        response.setMinOrderValue(entity.getMinOrderValue());
        response.setStartDate(entity.getStartDate());
        response.setExpiryDate(entity.getExpiryDate());
        response.setUsageLimit(entity.getUsageLimit());
        response.setUsedCount(entity.getUsedCount());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
