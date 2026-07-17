package com.api.batterymantra.dto.manufacturer;

import lombok.Data;

@Data
public class UpdateManufacturerRequest {
    private String name;
    private String logoUrl;
    private Integer displayOrder;
}
