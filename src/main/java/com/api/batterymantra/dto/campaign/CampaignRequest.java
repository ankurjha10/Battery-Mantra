package com.api.batterymantra.dto.campaign;

import com.api.batterymantra.entity.enums.BadgeTheme;
import com.api.batterymantra.entity.enums.CampaignDiscountType;
import com.api.batterymantra.entity.enums.CampaignTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {

    @NotBlank(message = "Campaign name is required")
    private String name;

    private String slug;

    private String tagline;

    private String description;

    private String badgeText;

    @Builder.Default
    private BadgeTheme badgeTheme = BadgeTheme.BRAND_RED;

    private String desktopBannerUrl;

    private String mobileBannerUrl;

    @NotNull(message = "Start date is required")
    private OffsetDateTime startDate;

    @NotNull(message = "End date is required")
    private OffsetDateTime endDate;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    private Boolean showCountdown = true;

    @Builder.Default
    private Boolean showInAnnouncement = true;

    @Builder.Default
    private Boolean showOnHome = true;

    @NotNull(message = "Target type is required")
    private CampaignTargetType targetType;

    @NotNull(message = "Discount type is required")
    private CampaignDiscountType discountType;

    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    private BigDecimal maxDiscountCap;

    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private String couponCode;

    @Builder.Default
    private Set<UUID> categoryIds = new HashSet<>();

    @Builder.Default
    private Set<UUID> brandIds = new HashSet<>();

    @Builder.Default
    private Set<UUID> productIds = new HashSet<>();

    @Builder.Default
    private Set<UUID> excludedProductIds = new HashSet<>();

    @Builder.Default
    private Map<UUID, BigDecimal> productDiscounts = new HashMap<>();

    @Builder.Default
    private Map<UUID, BigDecimal> productDiscounts = new HashMap<>();
}
