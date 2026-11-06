package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.BlogStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "blog_posts",
        indexes = {
                @Index(name = "idx_blog_posts_slug", columnList = "slug"),
                @Index(name = "idx_blog_posts_status", columnList = "status"),
                @Index(name = "idx_blog_posts_category", columnList = "category"),
                @Index(name = "idx_blog_posts_published_at", columnList = "published_at DESC")
        }
)
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @NotBlank
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_image", length = 512)
    private String coverImage;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 255)
    private String tags;

    @Builder.Default
    @Column(name = "author_name", length = 100)
    private String authorName = "Battery Mantra Team";

    @Column(name = "author_image", length = 512)
    private String authorImage;

    @Builder.Default
    @Column(name = "read_time_minutes")
    private Integer readTimeMinutes = 4;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogStatus status = BlogStatus.DRAFT;

    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // SEO Metadata
    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "meta_keywords", length = 512)
    private String metaKeywords;

    @Column(name = "canonical_url", length = 512)
    private String canonicalUrl;

    @Column(name = "og_image", length = 512)
    private String ogImage;

    // E-Commerce Conversion: Specific Products, Category, Brand, or Manufacturer CTA
    @Builder.Default
    @Column(name = "recommendation_type", length = 20)
    private String recommendationType = "PRODUCTS";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_product_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> relatedProductIds = new ArrayList<>();

    @Column(name = "related_category_id")
    private UUID relatedCategoryId;

    @Column(name = "related_category_name", length = 100)
    private String relatedCategoryName;

    @Column(name = "related_category_slug", length = 100)
    private String relatedCategorySlug;

    @Column(name = "related_brand_id")
    private UUID relatedBrandId;

    @Column(name = "related_brand_name", length = 100)
    private String relatedBrandName;

    @Column(name = "related_brand_slug", length = 100)
    private String relatedBrandSlug;

    @Column(name = "related_manufacturer_id")
    private UUID relatedManufacturerId;

    @Column(name = "related_manufacturer_name", length = 100)
    private String relatedManufacturerName;
}

