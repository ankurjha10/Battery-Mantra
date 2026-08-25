package com.api.batterymantra.dto.engineer;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class EngineerInventoryResponse {
    private UUID id;
    private UUID engineerId;
    private UUID productId;
    private String productName;
    private int quantity;
}
