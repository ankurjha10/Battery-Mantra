package com.api.batterymantra.dto.manufacturer;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ManufacturerResponse {
    private UUID id;
    private String name;
    private String logoUrl;
    private Integer displayOrder;
    private String description;
    private List<CategoryInfo> categories;
    private Integer vehicleCount;

    @Data
    public static class CategoryInfo {
        private UUID categoryId;
        private String categoryName;
    }
}
