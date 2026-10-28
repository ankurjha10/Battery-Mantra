package com.api.batterymantra.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryDto {
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
}
