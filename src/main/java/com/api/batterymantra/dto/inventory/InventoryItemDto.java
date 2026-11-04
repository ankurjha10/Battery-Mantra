package com.api.batterymantra.dto.inventory;

import com.api.batterymantra.entity.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {
    private UUID productId;
    private String productName;
    private String productImage;
    private String brandName;
    private String categoryName;
    private String capacity;
    private BigDecimal productPrice;
    private int productStock;
    private int lowStockThreshold;
    private StockStatus stockStatus;
    private LocalDateTime updatedAt;
}

