package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserVehicleRequest {

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotBlank(message = "Model name is required")
    private String modelName;

    @NotBlank(message = "Fuel type is required")
    private String fuelType;

    private String nickname;
}
