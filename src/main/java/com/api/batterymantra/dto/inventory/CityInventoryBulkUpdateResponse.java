package com.api.batterymantra.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityInventoryBulkUpdateResponse {

    private UUID cityId;
    private int updatedCount;
    private String message;
}

