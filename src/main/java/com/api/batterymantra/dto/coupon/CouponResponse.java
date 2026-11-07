package com.api.batterymantra.dto.coupon;

import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
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
public class CouponResponse {
    private String couponId;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private Double maxDiscountAmount;
    private Double minOrderValue;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Targeting
    private CouponTargetType targetType;
    private List<String> targetCategoryIds;
    private List<String> targetCategoryNames;
    private List<String> targetBrandIds;
    private List<String> targetBrandNames;
    private List<String> targetProductIds;
    private List<String> excludedProductIds;
    private Boolean displayOnPdp;
    private String pdpHighlightText;

    // Convenience alias for id
    public String getId() {
        return couponId;
    }

    public void setId(String id) {
        this.couponId = id;
    }
}
