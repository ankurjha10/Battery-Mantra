package com.api.batterymantra.dto.brandcontent;

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
public class BrandContentResponse {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String normalShortDescription;
    private String normalDescription;
    private String normalSeoTitle;
    private String dealerShortDescription;
    private String dealerDescription;
    private String dealerSeoTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
