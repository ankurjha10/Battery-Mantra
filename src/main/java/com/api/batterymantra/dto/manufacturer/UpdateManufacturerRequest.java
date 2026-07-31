package com.api.batterymantra.dto.manufacturer;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateManufacturerRequest {
    private String name;
    private String logoUrl;
    private Integer displayOrder;
    private String description;
    private List<UUID> categoryIds;
}
