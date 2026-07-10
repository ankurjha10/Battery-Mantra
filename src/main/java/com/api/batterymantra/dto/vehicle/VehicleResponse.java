package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.FuelType;
import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class VehicleResponse {
    private UUID vehicleId;
    private String make;
    private String model;
    private Integer yearFrom;
    private Integer yearTo;
    private FuelType fuelType;
    private VehicleType vehicleType;
    private String imageUrl;
}
