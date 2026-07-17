package com.api.batterymantra.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateManufacturerRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String logoUrl;
    
    private Integer displayOrder;
}
