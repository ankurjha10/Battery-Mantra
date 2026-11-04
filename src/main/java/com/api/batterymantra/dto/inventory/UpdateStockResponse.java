package com.api.batterymantra.dto.inventory;

import com.api.batterymantra.entity.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockResponse {
    private UUID productId;
    private int productStock;
    private StockStatus stockStatus;
    private String message;
}

