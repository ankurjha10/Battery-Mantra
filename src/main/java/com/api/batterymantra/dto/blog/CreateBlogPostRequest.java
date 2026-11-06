package com.api.batterymantra.dto.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBlogPostRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String slug; // If blank, backend should slugify(title)

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content; // HTML content

    private String coverImage;

    @NotBlank(message = "Category is required")
    private String category;

    private String tags;

    private String authorName;

    private String authorImage;

    private Integer readTimeMinutes;

    private String status; // DRAFT or PUBLISHED (defaults to DRAFT)

    private Boolean isFeatured;

    private List<UUID> relatedProductIds;

    // Recommendations & Attached CTAs
    private String recommendationType; // NONE, PRODUCTS, CATEGORY, BRAND, MANUFACTURER
    private UUID relatedCategoryId;
    private String relatedCategoryName;
    private String relatedCategorySlug;
    private UUID relatedBrandId;
    private String relatedBrandName;
    private String relatedBrandSlug;
    private UUID relatedManufacturerId;
    private String relatedManufacturerName;

    // SEO
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String canonicalUrl;
    private String ogImage;
}

