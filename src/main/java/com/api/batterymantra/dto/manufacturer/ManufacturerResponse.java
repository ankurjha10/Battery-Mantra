package com.api.batterymantra.dto.manufacturer;

import lombok.Data;
import java.util.UUID;

@Data
public class ManufacturerResponse {
    private UUID id;
    private String name;
    private String logoUrl;
    private Integer displayOrder;
}
