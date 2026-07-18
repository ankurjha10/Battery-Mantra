package com.api.batterymantra.dto.capacity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CapacityResponse {
    private UUID capacityId;
    private UUID categoryId;
    private String capacityName;
}
