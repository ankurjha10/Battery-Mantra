package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class VehicleResponse {
    private UUID vehicleId;
    private String make;
    private String model;
    private UUID fuelId;
    private String fuelName;
    private VehicleType vehicleType;
    private String imageUrl;
    private String capacity;

    private UUID categoryId;
    private UUID manufacturerId;
    private String description;
    private String shortDescription;
    private String shortDescriptionDealer;

    private com.api.batterymantra.entity.SeoMetadata seo;
}
