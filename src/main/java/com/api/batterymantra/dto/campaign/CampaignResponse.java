package com.api.batterymantra.dto.campaign;

import com.api.batterymantra.entity.enums.BadgeTheme;
import com.api.batterymantra.entity.enums.CampaignDiscountType;
import com.api.batterymantra.entity.enums.CampaignTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private UUID id;
    private String name;
    private String slug;
    private String tagline;
    private String description;
    private String badgeText;
    private BadgeTheme badgeTheme;
    private String desktopBannerUrl;
    private String mobileBannerUrl;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private Boolean isActive;
    private Integer priority;
    private Boolean showCountdown;
    private Boolean showInAnnouncement;
    private Boolean showOnHome;
    private CampaignTargetType targetType;
    private CampaignDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountCap;
    private BigDecimal minOrderValue;
    private String couponCode;
    private Set<UUID> categoryIds;
    private Set<UUID> brandIds;
    private Set<UUID> productIds;
    private Set<UUID> excludedProductIds;
    private Map<UUID, BigDecimal> productDiscounts;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String status; // "ACTIVE", "SCHEDULED", "EXPIRED", "INACTIVE"
}
