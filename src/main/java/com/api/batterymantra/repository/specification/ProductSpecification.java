package com.api.batterymantra.repository.specification;

import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.Vehicle;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

    public static Specification<Product> hasCategoryIdIn(java.util.List<UUID> categoryIds) {
        return (root, query, cb) ->
                root.get("productCategory").get("categoryId").in(categoryIds);
    }

    public static Specification<Product> hasBrandId(UUID brandId) {
        return (root, query, cb) ->
                cb.equal(root.get("brand").get("brandId"), brandId);
    }

    public static Specification<Product> hasCapacityIn(java.util.List<String> capacities) {
        return (root, query, cb) -> root.get("capacity").in(capacities);
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
