package com.api.batterymantra.dto.category;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateCategoryRequest {
    public String categoryName;
    public String categoryDescription;
    public String iconUrl;
    public Integer displayOrder;
    public UUID parentId;
}
