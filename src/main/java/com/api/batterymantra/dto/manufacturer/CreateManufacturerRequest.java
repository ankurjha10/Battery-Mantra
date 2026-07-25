package com.api.batterymantra.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateManufacturerRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String logoUrl;
    
    private Integer displayOrder;

    private List<UUID> categoryIds;
}
