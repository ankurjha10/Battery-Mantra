package com.api.batterymantra.dto.banner;

import lombok.Data;

import java.util.UUID;

@Data
public class BannerResponse {
    private UUID bannerId;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Boolean isActive;
    private Integer displayOrder;
}
