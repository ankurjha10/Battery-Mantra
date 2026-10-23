package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateVehicleRequest {
    private String make;
    private String model;
    private UUID fuelId;
    private VehicleType vehicleType;
    private String imageUrl;
    private String capacity;

    private UUID categoryId;
    private UUID manufacturerId;
    private String description;
    private String slug;
}
