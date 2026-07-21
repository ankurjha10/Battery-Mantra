package com.api.batterymantra.dto.cms;

import com.api.batterymantra.entity.SeoMetadata;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCmsPageRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String subTitle;
    private String image1;
    private String image2;
    private String content;
    private String content2;
    
    private boolean isActive = true;
    
    private SeoMetadata seo;
}
