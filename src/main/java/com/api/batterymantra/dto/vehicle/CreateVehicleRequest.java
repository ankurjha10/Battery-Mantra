package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.FuelType;
import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Data;

@Data
public class CreateVehicleRequest {
    private String make;
    private String model;
    private FuelType fuelType;
    private VehicleType vehicleType;
    private String imageUrl;
}
