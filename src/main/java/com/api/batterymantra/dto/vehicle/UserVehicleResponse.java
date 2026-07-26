package com.api.batterymantra.dto.vehicle;

import com.api.batterymantra.entity.enums.VehicleType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserVehicleResponse {
    private UUID id;
    private VehicleType vehicleType;
    private String manufacturer;
    private String modelName;
    private String fuelType;
    private String nickname;
    private LocalDateTime createdAt;
}
