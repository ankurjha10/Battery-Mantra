package com.api.batterymantra.dto.category;

import lombok.Data;

import java.util.List;
import java.util.UUID;
import com.api.batterymantra.enums.ClickAction;

@Data
public class CategoryDetailResponse {
    public UUID categoryId;
    public String categoryName;
    public String categoryDescription;
    public String iconUrl;
    public Integer displayOrder;
    public UUID parentId;
    public ClickAction clickAction;
    public List<CategoryListResponse> subCategories;
    public List<UUID> products;
}
