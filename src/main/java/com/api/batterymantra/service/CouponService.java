package com.api.batterymantra.service;

import com.api.batterymantra.dto.coupon.ApplyCouponRequest;
import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.dto.coupon.CouponRequest;
import com.api.batterymantra.dto.coupon.CouponResponse;
import com.api.batterymantra.entity.Coupon;
import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
import com.api.batterymantra.repository.CouponRepository;
import com.api.batterymantra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;

    public List<CouponResponse> getActivePublicCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> activeCoupons = couponRepository.findActivePublicCoupons(now);
        return activeCoupons.stream().map(this::mapEntityToResponse).toList();
    }

    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {
        if (request == null || request.getCode() == null || request.getCode().trim().isBlank()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon code is required")
                    .finalTotal(request != null && request.getCartTotal() != null ? request.getCartTotal() : 0.0)
                    .build();
        }

        String code = request.getCode().trim().toUpperCase();
        Double cartTotal = request.getCartTotal() != null ? request.getCartTotal() : 0.0;
        List<ApplyCouponRequest.ApplyCouponCartItemDto> cartItems = request.getCartItems();

        Optional<Coupon> optionalCoupon = couponRepository.findByCodeIgnoreCase(code);
        if (optionalCoupon.isEmpty()) {
            return ApplyCouponResponse.builder()
                    .isValid(false)
                    .code(code)
                    .discountAmount(0.0)
                    .message("Invalid coupon code")
                    .finalTotal(cartTotal)
                    .build();
        }

        Coupon coupon = optionalCoupon.get();
        String couponIdStr = coupon.getId() != null ? coupon.getId().toString() : null;

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon is inactive")
                    .finalTotal(cartTotal)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon is not yet active")
                    .finalTotal(cartTotal)
                    .build();
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon has expired")
                    .finalTotal(cartTotal)
                    .build();
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Coupon usage limit exceeded")
                    .finalTotal(cartTotal)
                    .build();
        }

        if (coupon.getMinOrderValue() != null && cartTotal < coupon.getMinOrderValue()) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message("Cart total must be at least " + coupon.getMinOrderValue() + " to use this coupon")
                    .finalTotal(cartTotal)
                    .build();
        }

        // Enrich cart items from productRepository if needed
        enrichCartItemsIfNecessary(cartItems);

        // Targeting Validation
        CouponTargetType targetType = coupon.getTargetType() != null ? coupon.getTargetType() : CouponTargetType.ALL_PRODUCTS;
        boolean isTargetValid = true;
        String targetErrorMessage = null;

        switch (targetType) {
            case ALL_PRODUCTS:
                isTargetValid = true;
                break;

            case CATEGORIES:
                if (cartItems == null || cartItems.isEmpty()) {
                    isTargetValid = false;
                    targetErrorMessage = "Coupon is only valid for eligible product categories";
                } else {
                    boolean matchesCategory = cartItems.stream().anyMatch(item ->
                            listContainsId(coupon.getTargetCategoryIds(), item.getCategoryId())
                    );
                    if (!matchesCategory) {
                        isTargetValid = false;
                        targetErrorMessage = "Coupon is only valid for eligible product categories";
                    }
                }
                break;

            case BRANDS:
                if (cartItems == null || cartItems.isEmpty()) {
                    isTargetValid = false;
                    targetErrorMessage = "Coupon is only valid for eligible brands";
                } else {
                    boolean matchesBrand = cartItems.stream().anyMatch(item ->
                            listContainsId(coupon.getTargetBrandIds(), item.getBrandId())
                    );
                    if (!matchesBrand) {
                        isTargetValid = false;
                        targetErrorMessage = "Coupon is only valid for eligible brands";
                    }
                }
                break;

            case CUSTOM_PRODUCTS:
                if (cartItems == null || cartItems.isEmpty()) {
                    isTargetValid = false;
                    targetErrorMessage = "Coupon is only valid for eligible products";
                } else {
                    boolean matchesProduct = cartItems.stream().anyMatch(item ->
                            listContainsId(coupon.getTargetProductIds(), item.getProductId())
                    );
                    if (!matchesProduct) {
                        isTargetValid = false;
                        targetErrorMessage = "Coupon is only valid for eligible products";
                    }
                }
                break;

            case EXCLUDED_PRODUCTS:
                if (cartItems != null && !cartItems.isEmpty() && coupon.getExcludedProductIds() != null && !coupon.getExcludedProductIds().isEmpty()) {
                    boolean containsExcluded = cartItems.stream().anyMatch(item ->
                            listContainsId(coupon.getExcludedProductIds(), item.getProductId())
                    );
                    if (containsExcluded) {
                        isTargetValid = false;
                        targetErrorMessage = "Coupon cannot be applied because cart contains excluded products";
                    }
                }
                break;
        }

        if (!isTargetValid) {
            return ApplyCouponResponse.builder()
                    .couponId(couponIdStr)
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .isValid(false)
                    .discountAmount(0.0)
                    .message(targetErrorMessage != null ? targetErrorMessage : "Coupon is not applicable to items in your cart")
                    .finalTotal(cartTotal)
                    .build();
        }

        // Calculate discount amount
        double discountAmount = 0.0;
        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discountAmount = Math.min(coupon.getDiscountValue(), cartTotal);
        } else if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = (cartTotal * coupon.getDiscountValue()) / 100.0;
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount() > 0) {
                discountAmount = Math.min(discountAmount, coupon.getMaxDiscountAmount());
            }
            discountAmount = Math.min(discountAmount, cartTotal);
        }

        return ApplyCouponResponse.builder()
                .couponId(couponIdStr)
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .isValid(true)
                .message("Coupon applied successfully")
                .finalTotal(cartTotal - discountAmount)
                .build();
    }

    // Overload for backward compatibility
    public ApplyCouponResponse applyCoupon(String code, Double cartTotal) {
        return applyCoupon(ApplyCouponRequest.builder()
                .code(code)
                .cartTotal(cartTotal)
                .build());
    }

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
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Cannot delete coupon because it has been used in orders. Please deactivate it instead.");
        }
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .toList();
    }

    public CouponResponse getCouponById(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found with id: " + id));
        return mapEntityToResponse(coupon);
    }

    public void incrementCouponUsage(String code) {
        if (code == null || code.isBlank()) return;
        couponRepository.findByCodeIgnoreCase(code.trim()).ifPresent(coupon -> {
            coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
            couponRepository.save(coupon);
        });
    }

    private void enrichCartItemsIfNecessary(List<ApplyCouponRequest.ApplyCouponCartItemDto> cartItems) {
        if (cartItems == null || cartItems.isEmpty() || productRepository == null) {
            return;
        }
        for (ApplyCouponRequest.ApplyCouponCartItemDto item : cartItems) {
            if ((item.getCategoryId() == null || item.getBrandId() == null) && item.getProductId() != null) {
                try {
                    UUID pid = UUID.fromString(item.getProductId().trim());
                    productRepository.findById(pid).ifPresent(p -> {
                        if (item.getCategoryId() == null && p.getProductCategory() != null && p.getProductCategory().getCategoryId() != null) {
                            item.setCategoryId(p.getProductCategory().getCategoryId().toString());
                        }
                        if (item.getBrandId() == null && p.getBrand() != null && p.getBrand().getBrandId() != null) {
                            item.setBrandId(p.getBrand().getBrandId().toString());
                        }
                    });
                } catch (IllegalArgumentException ignored) {
                    // Non-UUID productId string (e.g. test dummy 'prod-uuid-1'), retain candidate
                }
            }
        }
    }

    private boolean listContainsId(List<String> list, String candidateId) {
        if (list == null || list.isEmpty() || candidateId == null || candidateId.isBlank()) {
            return false;
        }
        String cleanCandidate = candidateId.trim().toLowerCase();
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(cleanCandidate::equals);
    }

    private void mapRequestToEntity(CouponRequest request, Coupon entity) {
        entity.setCode(request.getCode());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : 0.0);
        entity.setStartDate(request.getStartDate());
        entity.setExpiryDate(request.getExpiryDate());
        entity.setUsageLimit(request.getUsageLimit());

        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        } else if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }

        if (request.getTargetType() != null) {
            entity.setTargetType(request.getTargetType());
        } else if (entity.getTargetType() == null) {
            entity.setTargetType(CouponTargetType.ALL_PRODUCTS);
        }

        entity.setTargetCategoryIds(request.getTargetCategoryIds() != null ? new ArrayList<>(request.getTargetCategoryIds()) : new ArrayList<>());
        entity.setTargetCategoryNames(request.getTargetCategoryNames() != null ? new ArrayList<>(request.getTargetCategoryNames()) : new ArrayList<>());
        entity.setTargetBrandIds(request.getTargetBrandIds() != null ? new ArrayList<>(request.getTargetBrandIds()) : new ArrayList<>());
        entity.setTargetBrandNames(request.getTargetBrandNames() != null ? new ArrayList<>(request.getTargetBrandNames()) : new ArrayList<>());
        entity.setTargetProductIds(request.getTargetProductIds() != null ? new ArrayList<>(request.getTargetProductIds()) : new ArrayList<>());
        entity.setExcludedProductIds(request.getExcludedProductIds() != null ? new ArrayList<>(request.getExcludedProductIds()) : new ArrayList<>());

        if (request.getDisplayOnPdp() != null) {
            entity.setDisplayOnPdp(request.getDisplayOnPdp());
        } else if (entity.getDisplayOnPdp() == null) {
            entity.setDisplayOnPdp(true);
        }

        entity.setPdpHighlightText(request.getPdpHighlightText());
    }

    private CouponResponse mapEntityToResponse(Coupon entity) {
        return CouponResponse.builder()
                .couponId(entity.getId() != null ? entity.getId().toString() : (entity.getCouponId() != null ? entity.getCouponId().toString() : null))
                .code(entity.getCode())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .minOrderValue(entity.getMinOrderValue())
                .startDate(entity.getStartDate())
                .expiryDate(entity.getExpiryDate())
                .usageLimit(entity.getUsageLimit())
                .usedCount(entity.getUsedCount())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .targetType(entity.getTargetType() != null ? entity.getTargetType() : CouponTargetType.ALL_PRODUCTS)
                .targetCategoryIds(entity.getTargetCategoryIds() != null ? entity.getTargetCategoryIds() : new ArrayList<>())
                .targetCategoryNames(entity.getTargetCategoryNames() != null ? entity.getTargetCategoryNames() : new ArrayList<>())
                .targetBrandIds(entity.getTargetBrandIds() != null ? entity.getTargetBrandIds() : new ArrayList<>())
                .targetBrandNames(entity.getTargetBrandNames() != null ? entity.getTargetBrandNames() : new ArrayList<>())
                .targetProductIds(entity.getTargetProductIds() != null ? entity.getTargetProductIds() : new ArrayList<>())
                .excludedProductIds(entity.getExcludedProductIds() != null ? entity.getExcludedProductIds() : new ArrayList<>())
                .displayOnPdp(entity.getDisplayOnPdp() != null ? entity.getDisplayOnPdp() : true)
                .pdpHighlightText(entity.getPdpHighlightText())
                .build();
    }
}
