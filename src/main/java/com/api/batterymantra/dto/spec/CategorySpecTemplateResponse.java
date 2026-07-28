package com.api.batterymantra.dto.spec;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CategorySpecTemplateResponse {

    private UUID categoryId;
    private String categoryName;
    private List<SpecGroupDto> specGroups;

    @Data
    @Builder
    public static class SpecGroupDto {
        private UUID specCategoryId;
        private String specCategoryName;
        private List<SpecAttributeDto> attributes;
    }

    @Data
    @Builder
    public static class SpecAttributeDto {
        private UUID attributeId;
        private String attributeName;
        private List<SpecUnitDto> availableUnits; // List of predefined unit options
    }

    @Data
    @Builder
    public static class SpecUnitDto {
        private UUID unitId;
        private String unitValue;
    }
}
