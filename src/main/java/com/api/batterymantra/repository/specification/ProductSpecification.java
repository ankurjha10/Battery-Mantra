package com.api.batterymantra.repository.specification;

import java.util.List;

import com.api.batterymantra.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecification {

    private ProductSpecification() {
        // utility class
    }

    public static Specification<Product> hasCategoryId(UUID categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("productCategory").get("categoryId"), categoryId);
    }

    public static Specification<Product> hasCategoryIdIn(List<UUID> categoryIds) {
        return (root, query, cb) ->
                root.get("productCategory").get("categoryId").in(categoryIds);
    }

    public static Specification<Product> hasBrandIdIn(List<UUID> brandIds) {
        return (root, query, cb) ->
                root.get("brand").get("brandId").in(brandIds);
    }

    public static Specification<Product> hasCapacityIn(List<String> capacities) {
        return (root, query, cb) -> root.get("capacity").in(capacities);
    }

    public static Specification<Product> hasWarrantyIn(List<String> warranties) {
        return (root, query, cb) -> {
            var specUnitsJoin = root.join("specUnits");
            return specUnitsJoin.get("unitValue").in(warranties);
            // Assuming unitValue contains the "60 Months" string.
        };
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("productPrice"), minPrice);
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("productPrice"), maxPrice);
    }

    /**
     * Filter by JSONB spec field.
     * Uses PostgreSQL native JSONB operator via a native SQL function expression.
     * Example: specs->>'voltage' = '12V'
     */
    public static Specification<Product> hasSpec(String specKey, String specValue) {
        return (root, query, cb) ->
                cb.equal(
                        cb.function("jsonb_extract_path_text", String.class,
                                root.get("spec"), cb.literal(specKey)),
                        specValue
                );
    }

    public static Specification<Product> hasNameContaining(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%");
    }
}
