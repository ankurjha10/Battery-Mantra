package com.api.batterymantra.dto.spec;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SpecUnitResponse {

    private UUID id;
    private String value;
    private UUID specAttributeId;
    private String specAttributeName;
    private UUID specCategoryId;
    private String specCategoryName;
    private UUID categoryId;
    private String categoryName;
}
