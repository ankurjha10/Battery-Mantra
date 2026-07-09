package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.FuelType;
import lombok.Data;

@Data
public class CreateVehicleRequest {
    private String make;
    private String model;
    private Integer yearFrom;
    private Integer yearTo;
    private FuelType fuelType;
    private String imageUrl;
}
