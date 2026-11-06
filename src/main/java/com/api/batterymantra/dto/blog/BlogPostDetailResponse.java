package com.api.batterymantra.dto.blog;

import com.api.batterymantra.dto.product.ProductListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostDetailResponse {

    private UUID id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String coverImage;
    private String category;
    private String tags;
    private String authorName;
    private String authorImage;
    private Integer readTimeMinutes;
    private String status;
    private Boolean isFeatured;
    private Long viewCount;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // SEO
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String canonicalUrl;
    private String ogImage;

    // Direct Hydrated Products for Buy Now cards inside article
    private List<ProductListResponse> relatedProducts;

    // Raw related product IDs
    private List<UUID> relatedProductIds;
}

