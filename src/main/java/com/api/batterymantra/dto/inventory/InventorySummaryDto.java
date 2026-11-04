package com.api.batterymantra.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDto {
    private long totalSkus;
    private long inStockCount;
    private long lowStockCount;
    private long outOfStockCount;
    private int lowStockThreshold;
}

