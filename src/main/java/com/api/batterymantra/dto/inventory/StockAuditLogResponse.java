package com.api.batterymantra.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAuditLogResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String changeType;
    private int quantityChange;
    private int previousStock;
    private int newStock;
    private UUID orderId;
    private String performedBy;
    private String notes;
    private LocalDateTime createdAt;
}

