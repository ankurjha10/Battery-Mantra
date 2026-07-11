package com.api.batterymantra.dto.banner;

import lombok.Data;

@Data
public class UpdateBannerRequest {
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Boolean isActive;
    private Integer displayOrder;
}
