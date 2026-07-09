package com.api.batterymantra.dto.category;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CategoryListResponse {
    public UUID categoryId;
    public String categoryName;
    public String categoryDescription;
    public String iconUrl;
    public Integer displayOrder;
    public UUID parentId;
    public List<CategoryListResponse> subCategories;
}
