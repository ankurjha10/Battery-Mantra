package com.api.batterymantra.dto.faq;

import com.api.batterymantra.enums.FaqPageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FaqResponse {
    private UUID faqId;
    private FaqPageType pageType;
    private String title;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
