package com.api.batterymantra.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerBulkInventoryUpdateRequest {

    @NotNull(message = "City ID is required")
    private UUID cityId;

    @NotEmpty(message = "Updates list cannot be empty")
    @Valid
    private List<CityProductStockItem> updates;
}

