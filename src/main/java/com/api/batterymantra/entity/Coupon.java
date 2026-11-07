package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType; // FLAT, PERCENTAGE

    @Column(nullable = false)
    private Double discountValue;

    private Double maxDiscountAmount;
    private Double minOrderValue;
    private Integer usageLimit;

    @Builder.Default
    private Integer usedCount = 0;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime startDate;
    private LocalDateTime expiryDate;

    // --- Targeting & PDP Display Fields ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CouponTargetType targetType = CouponTargetType.ALL_PRODUCTS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetCategoryIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetCategoryNames = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetBrandIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetBrandNames = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetProductIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> excludedProductIds = new ArrayList<>();

    @Builder.Default
    private Boolean displayOnPdp = true;

    @Column(length = 255)
    private String pdpHighlightText;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Backwards compatibility alias for couponId
    public UUID getCouponId() {
        return id;
    }

    public void setCouponId(UUID couponId) {
        this.id = couponId;
    }

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.code != null) this.code = this.code.trim().toUpperCase();
        if (this.usedCount == null) this.usedCount = 0;
        if (this.isActive == null) this.isActive = true;
        if (this.targetType == null) this.targetType = CouponTargetType.ALL_PRODUCTS;
        if (this.displayOnPdp == null) this.displayOnPdp = true;
        if (this.targetCategoryIds == null) this.targetCategoryIds = new ArrayList<>();
        if (this.targetCategoryNames == null) this.targetCategoryNames = new ArrayList<>();
        if (this.targetBrandIds == null) this.targetBrandIds = new ArrayList<>();
        if (this.targetBrandNames == null) this.targetBrandNames = new ArrayList<>();
        if (this.targetProductIds == null) this.targetProductIds = new ArrayList<>();
        if (this.excludedProductIds == null) this.excludedProductIds = new ArrayList<>();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.code != null) this.code = this.code.trim().toUpperCase();
        if (this.targetCategoryIds == null) this.targetCategoryIds = new ArrayList<>();
        if (this.targetCategoryNames == null) this.targetCategoryNames = new ArrayList<>();
        if (this.targetBrandIds == null) this.targetBrandIds = new ArrayList<>();
        if (this.targetBrandNames == null) this.targetBrandNames = new ArrayList<>();
        if (this.targetProductIds == null) this.targetProductIds = new ArrayList<>();
        if (this.excludedProductIds == null) this.excludedProductIds = new ArrayList<>();
    }
}
