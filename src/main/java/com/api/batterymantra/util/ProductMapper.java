package com.api.batterymantra.util;

import com.api.batterymantra.dto.category.CategorySummaryDto;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductMapper {

    private ProductMapper() {
        // utility class
    }

    /**
     * Maps all categories associated with a product (primary category first, followed by any linked categories).
     * Guarantees non-duplicate entries with deterministic order.
     */
    public static List<CategorySummaryDto> mapCategories(Product product) {
        if (product == null) {
            return new ArrayList<>();
        }

        Map<UUID, CategorySummaryDto> categoryMap = new LinkedHashMap<>();

        // 1. Primary category always placed first
        Category primary = product.getProductCategory();
        if (primary != null && primary.getCategoryId() != null) {
            categoryMap.put(primary.getCategoryId(), CategorySummaryDto.builder()
                    .categoryId(primary.getCategoryId())
                    .categoryName(primary.getCategoryName())
                    .categorySlug(primary.getSlug())
                    .build());
        }

        // 2. All linked categories from many-to-many relationship
        if (product.getCategories() != null) {
            for (Category c : product.getCategories()) {
                if (c != null && c.getCategoryId() != null && !categoryMap.containsKey(c.getCategoryId())) {
                    categoryMap.put(c.getCategoryId(), CategorySummaryDto.builder()
                            .categoryId(c.getCategoryId())
                            .categoryName(c.getCategoryName())
                            .categorySlug(c.getSlug())
                            .build());
                }
            }
        }

        return new ArrayList<>(categoryMap.values());
    }
}
