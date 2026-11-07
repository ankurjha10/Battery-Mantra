package com.api.batterymantra.dto.coupon;

import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    private Double discountValue;

    private Double maxDiscountAmount;
    private Double minOrderValue;
    private Integer usageLimit;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;

    // Targeting & PDP Display
    private CouponTargetType targetType;
    private List<String> targetCategoryIds;
    private List<String> targetCategoryNames;
    private List<String> targetBrandIds;
    private List<String> targetBrandNames;
    private List<String> targetProductIds;
    private List<String> excludedProductIds;
    private Boolean displayOnPdp;
    private String pdpHighlightText;
}
