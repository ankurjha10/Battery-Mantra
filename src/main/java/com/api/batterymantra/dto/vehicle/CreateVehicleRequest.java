package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Data;

@Data
public class CreateVehicleRequest {
    private String make;
    private String model;
    private java.util.UUID fuelId;
    private VehicleType vehicleType;
    private String imageUrl;
    private String capacity;
    
    private java.util.UUID categoryId;
    private java.util.UUID manufacturerId;
    private String description;
    private String shortDescription;
    private String shortDescriptionDealer;
    
    private com.api.batterymantra.entity.SeoMetadata seo;
}
