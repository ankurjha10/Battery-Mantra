package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.BadgeTheme;
import com.api.batterymantra.entity.enums.CampaignDiscountType;
import com.api.batterymantra.entity.enums.CampaignTargetType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaigns_active", columnList = "is_active, start_date, end_date"),
        @Index(name = "idx_campaigns_slug", columnList = "slug", unique = true)
})
public class Campaign {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(length = 500)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "badge_text", length = 100)
    private String badgeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_theme", length = 50)
    @Builder.Default
    private BadgeTheme badgeTheme = BadgeTheme.BRAND_RED;

    @Column(name = "desktop_banner_url", length = 1000)
    private String desktopBannerUrl;

    @Column(name = "mobile_banner_url", length = 1000)
    private String mobileBannerUrl;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "show_countdown")
    @Builder.Default
    private Boolean showCountdown = true;

    @Column(name = "show_in_announcement")
    @Builder.Default
    private Boolean showInAnnouncement = true;

    @Column(name = "show_on_home")
    @Builder.Default
    private Boolean showOnHome = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private CampaignTargetType targetType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 50)
    private CampaignDiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "max_discount_cap", precision = 10, scale = 2)
    private BigDecimal maxDiscountCap;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "campaign_categories", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<UUID> categoryIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "campaign_brands", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "brand_id")
    @Builder.Default
    private Set<UUID> brandIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "campaign_products", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<UUID> productIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "campaign_excluded_products", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<UUID> excludedProductIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_discounts", columnDefinition = "jsonb")
    @Builder.Default
    private Map<UUID, BigDecimal> productDiscounts = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
