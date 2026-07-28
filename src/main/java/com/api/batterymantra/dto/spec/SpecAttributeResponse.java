package com.api.batterymantra.dto.spec;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SpecAttributeResponse {

    private UUID id;
    private String name;
    private UUID specCategoryId;
    private String specCategoryName;
    private UUID categoryId;
    private String categoryName;
}
