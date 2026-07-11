package com.api.batterymantra.dto.banner;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBannerRequest {
    private String title;

    @NotBlank(message = "imageUrl is required")
    private String imageUrl;
    
    private String linkUrl;
    private Boolean isActive;
    private Integer displayOrder;
}
