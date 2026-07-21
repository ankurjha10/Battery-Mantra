package com.api.batterymantra.dto.cms;

import com.api.batterymantra.entity.SeoMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsPageDto {
    private UUID pageId;
    private String title;
    private String content;
    private boolean isActive;
    private SeoMetadata seo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
