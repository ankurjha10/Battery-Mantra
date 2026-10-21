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
            return specUnitsJoin.get("value").in(warranties);
            // Assuming value contains the "60 Months" string.
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

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String cleanKeyword = keyword.trim().toLowerCase();
            jakarta.persistence.criteria.Join<Product, com.api.batterymantra.entity.Brand> brandJoin = root.join("brand", jakarta.persistence.criteria.JoinType.LEFT);

            // 1. Direct partial match on searchKeywords or productName or brand
            jakarta.persistence.criteria.Predicate directMatch = cb.or(
                cb.like(cb.lower(root.get("productName")), "%" + cleanKeyword + "%"),
                cb.like(cb.lower(root.get("searchKeywords")), "%" + cleanKeyword + "%"),
                cb.like(cb.lower(brandJoin.get("brandName")), "%" + cleanKeyword + "%")
            );

            // 2. Multi-token split (e.g., "exide 35 wagonr")
            String[] tokens = cleanKeyword.split("\\s+");
            if (tokens.length > 1) {
                java.util.List<jakarta.persistence.criteria.Predicate> tokenPredicates = new java.util.ArrayList<>();
                for (String token : tokens) {
                    String pattern = "%" + token + "%";
                    tokenPredicates.add(cb.or(
                        cb.like(cb.lower(root.get("productName")), pattern),
                        cb.like(cb.lower(root.get("searchKeywords")), pattern),
                        cb.like(cb.lower(root.get("capacity")), pattern),
                        cb.like(cb.lower(brandJoin.get("brandName")), pattern)
                    ));
                }
                return cb.or(directMatch, cb.and(tokenPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
            } else {
                return directMatch;
            }
        };
    }

    public static Specification<Product> hasNameContaining(String keyword) {
        return hasKeyword(keyword);
    }

    /**
     * Sort by globalDisplayOrder ASC (nulls/zero last), then createdAt DESC.
     */
    public static Specification<Product> sortByGlobalOrder() {
        return (root, query, cb) -> {
            if (query != null) {
                query.orderBy(
                        cb.asc(cb.coalesce(root.get("globalDisplayOrder"), Integer.MAX_VALUE)),
                        cb.desc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * Sort by categoryDisplayOrder ASC (nulls/zero last), then createdAt DESC.
     */
    public static Specification<Product> sortByCategoryOrder() {
        return (root, query, cb) -> {
            if (query != null) {
                query.orderBy(
                        cb.asc(cb.coalesce(root.get("categoryDisplayOrder"), Integer.MAX_VALUE)),
                        cb.desc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * Sort by brandDisplayOrder ASC (nulls/zero last), then createdAt DESC.
     */
    public static Specification<Product> sortByBrandOrder() {
        return (root, query, cb) -> {
            if (query != null) {
                query.orderBy(
                        cb.asc(cb.coalesce(root.get("brandDisplayOrder"), Integer.MAX_VALUE)),
                        cb.desc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }

    /**
     * Sort by categoryBrandDisplayOrder ASC (nulls/zero last), then createdAt DESC.
     * Used when both category and brand filters are applied simultaneously.
     */
    public static Specification<Product> sortByCategoryBrandOrder() {
        return (root, query, cb) -> {
            if (query != null) {
                query.orderBy(
                        cb.asc(cb.coalesce(root.get("categoryBrandDisplayOrder"), Integer.MAX_VALUE)),
                        cb.desc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }
}
