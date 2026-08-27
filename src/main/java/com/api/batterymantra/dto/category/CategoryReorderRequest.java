package com.api.batterymantra.dto.category;

import lombok.Data;

import java.util.UUID;

@Data
public class CategoryReorderRequest {
    private UUID categoryId;
    private Integer displayOrder;
}
