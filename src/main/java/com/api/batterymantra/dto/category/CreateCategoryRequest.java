package com.api.batterymantra.dto.category;

import lombok.Data;

import java.util.UUID;
import com.api.batterymantra.enums.ClickAction;

@Data
public class CreateCategoryRequest {
    public String categoryName;
    public String categoryDescription;
    public String iconUrl;
    public Integer displayOrder;
    public UUID parentId;
    public ClickAction clickAction;
}
