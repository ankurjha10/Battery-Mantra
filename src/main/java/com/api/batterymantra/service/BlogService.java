package com.api.batterymantra.service;

import com.api.batterymantra.dto.blog.*;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.entity.BlogPost;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.enums.BlogStatus;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.BlogPostRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.specification.BlogPostSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final BlogViewCounterService blogViewCounterService;

    // ==========================================
    // 🔓 PUBLIC METHODS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<BlogPostListResponse> getPublicBlogs(
            int page,
            int size,
            String category,
            String keyword,
            String tag,
            Boolean isFeatured
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                size > 0 ? size : 12,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"))
        );

        Specification<BlogPost> spec = BlogPostSpecification.filter(
                category,
                keyword,
                tag,
                isFeatured,
                BlogStatus.PUBLISHED
        );

        return blogPostRepository.findAll(spec, pageable).map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public BlogPostDetailResponse getPublicBlogBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new ResourceNotFoundException("Blog post not found with slug: " + slug);
        }

        BlogPost post = blogPostRepository.findBySlugAndStatus(slug.trim(), BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));

        // Asynchronously increment view count
        blogViewCounterService.incrementViewCount(post.getId());

        return mapToDetailResponse(post);
    }

    @Transactional(readOnly = true)
    public List<BlogCategoryCountResponse> getCategoriesWithCounts() {
        return blogPostRepository.countPostsByCategory(BlogStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<BlogPostListResponse> getFeaturedBlogs() {
        return blogPostRepository.findTop3ByIsFeaturedTrueAndStatusOrderByPublishedAtDescCreatedAtDesc(BlogStatus.PUBLISHED)
                .stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 🔒 ADMIN METHODS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<BlogPostListResponse> getAdminBlogs(
            int page,
            int size,
            String category,
            String status,
            String keyword
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                size > 0 ? size : 12,
                Sort.by(Sort.Order.desc("createdAt"))
        );

        BlogStatus filterStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            filterStatus = parseStatus(status, null);
        }

        Specification<BlogPost> spec = BlogPostSpecification.filter(
                category,
                keyword,
                null,
                null,
                filterStatus
        );

        return blogPostRepository.findAll(spec, pageable).map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public BlogPostDetailResponse getAdminBlogById(UUID id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));
        return mapToDetailResponse(post);
    }

    @Transactional
    public BlogPostDetailResponse createBlog(CreateBlogPostRequest request) {
        String slug = generateUniqueSlug(request.getTitle(), request.getSlug(), null);
        int readTime = calculateReadTime(request.getContent(), request.getReadTimeMinutes());
        BlogStatus status = parseStatus(request.getStatus(), BlogStatus.DRAFT);

        Instant publishedAt = null;
        if (status == BlogStatus.PUBLISHED) {
            publishedAt = Instant.now();
        }

        String author = (request.getAuthorName() != null && !request.getAuthorName().trim().isEmpty())
                ? request.getAuthorName().trim()
                : "Battery Mantra Team";

        String recommendationType = (request.getRecommendationType() != null && !request.getRecommendationType().trim().isEmpty())
                ? request.getRecommendationType().trim().toUpperCase()
                : "PRODUCTS";

        BlogPost post = BlogPost.builder()
                .title(request.getTitle().trim())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .category(request.getCategory().trim())
                .tags(request.getTags())
                .authorName(author)
                .authorImage(request.getAuthorImage())
                .readTimeMinutes(readTime)
                .status(status)
                .isFeatured(Boolean.TRUE.equals(request.getIsFeatured()))
                .viewCount(0L)
                .publishedAt(publishedAt)
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .canonicalUrl(request.getCanonicalUrl())
                .ogImage(request.getOgImage())
                .recommendationType(recommendationType)
                .relatedProductIds(request.getRelatedProductIds() != null ? request.getRelatedProductIds() : new ArrayList<>())
                .relatedCategoryId(request.getRelatedCategoryId())
                .relatedCategoryName(request.getRelatedCategoryName())
                .relatedCategorySlug(request.getRelatedCategorySlug())
                .relatedBrandId(request.getRelatedBrandId())
                .relatedBrandName(request.getRelatedBrandName())
                .relatedBrandSlug(request.getRelatedBrandSlug())
                .relatedManufacturerId(request.getRelatedManufacturerId())
                .relatedManufacturerName(request.getRelatedManufacturerName())
                .build();

        BlogPost saved = blogPostRepository.save(post);
        return mapToDetailResponse(saved);
    }

    @Transactional
    public BlogPostDetailResponse updateBlog(UUID id, UpdateBlogPostRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));

        String slug = generateUniqueSlug(request.getTitle(), request.getSlug(), id);
        int readTime = calculateReadTime(request.getContent(), request.getReadTimeMinutes());
        BlogStatus newStatus = parseStatus(request.getStatus(), post.getStatus());

        if (newStatus == BlogStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        String author = (request.getAuthorName() != null && !request.getAuthorName().trim().isEmpty())
                ? request.getAuthorName().trim()
                : "Battery Mantra Team";

        post.setTitle(request.getTitle().trim());
        post.setSlug(slug);
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setCoverImage(request.getCoverImage());
        post.setCategory(request.getCategory().trim());
        post.setTags(request.getTags());
        post.setAuthorName(author);
        post.setAuthorImage(request.getAuthorImage());
        post.setReadTimeMinutes(readTime);
        post.setStatus(newStatus);
        post.setIsFeatured(Boolean.TRUE.equals(request.getIsFeatured()));
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDescription(request.getMetaDescription());
        post.setMetaKeywords(request.getMetaKeywords());
        post.setCanonicalUrl(request.getCanonicalUrl());
        post.setOgImage(request.getOgImage());
        if (request.getRecommendationType() != null && !request.getRecommendationType().trim().isEmpty()) {
            post.setRecommendationType(request.getRecommendationType().trim().toUpperCase());
        }
        post.setRelatedProductIds(request.getRelatedProductIds() != null ? request.getRelatedProductIds() : new ArrayList<>());
        post.setRelatedCategoryId(request.getRelatedCategoryId());
        post.setRelatedCategoryName(request.getRelatedCategoryName());
        post.setRelatedCategorySlug(request.getRelatedCategorySlug());
        post.setRelatedBrandId(request.getRelatedBrandId());
        post.setRelatedBrandName(request.getRelatedBrandName());
        post.setRelatedBrandSlug(request.getRelatedBrandSlug());
        post.setRelatedManufacturerId(request.getRelatedManufacturerId());
        post.setRelatedManufacturerName(request.getRelatedManufacturerName());

        BlogPost saved = blogPostRepository.save(post);
        return mapToDetailResponse(saved);
    }

    @Transactional
    public BlogPostDetailResponse updateBlogStatus(UUID id, UpdateBlogStatusRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));

        BlogStatus newStatus = parseStatus(request.getStatus(), post.getStatus());
        if (newStatus == BlogStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        post.setStatus(newStatus);
        BlogPost saved = blogPostRepository.save(post);
        return mapToDetailResponse(saved);
    }

    @Transactional
    public void deleteBlog(UUID id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));
        blogPostRepository.delete(post);
    }

    // ==========================================
    // 🛠️ HELPER METHODS
    // ==========================================

    public String generateUniqueSlug(String title, String inputSlug, UUID excludeId) {
        String baseSource = (inputSlug != null && !inputSlug.trim().isEmpty()) ? inputSlug : title;
        if (baseSource == null || baseSource.trim().isEmpty()) {
            baseSource = "blog-post";
        }

        String baseSlug = baseSource.toLowerCase().trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (baseSlug.isEmpty()) {
            baseSlug = "blog-post";
        }

        String candidateSlug = baseSlug;
        int counter = 1;
        while (isSlugTaken(candidateSlug, excludeId)) {
            candidateSlug = baseSlug + "-" + counter++;
        }

        return candidateSlug;
    }

    private boolean isSlugTaken(String slug, UUID excludeId) {
        if (excludeId == null) {
            return blogPostRepository.existsBySlug(slug);
        } else {
            return blogPostRepository.existsBySlugAndIdNot(slug, excludeId);
        }
    }

    public int calculateReadTime(String content, Integer readTimeMinutes) {
        if (readTimeMinutes != null && readTimeMinutes > 0) {
            return readTimeMinutes;
        }
        if (content == null || content.trim().isEmpty()) {
            return 1;
        }
        String textOnly = content.replaceAll("<[^>]*>", " ").trim();
        if (textOnly.isEmpty()) {
            return 1;
        }
        int words = textOnly.split("\\s+").length;
        return Math.max(1, words / 200);
    }

    private BlogStatus parseStatus(String statusStr, BlogStatus defaultStatus) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return defaultStatus != null ? defaultStatus : BlogStatus.DRAFT;
        }
        try {
            return BlogStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid blog status: " + statusStr + ". Allowed values: DRAFT, PUBLISHED, ARCHIVED.");
        }
    }

    public BlogPostListResponse mapToListResponse(BlogPost b) {
        return BlogPostListResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .slug(b.getSlug())
                .excerpt(b.getExcerpt())
                .coverImage(b.getCoverImage())
                .category(b.getCategory())
                .tags(b.getTags())
                .authorName(b.getAuthorName())
                .authorImage(b.getAuthorImage())
                .readTimeMinutes(b.getReadTimeMinutes())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .isFeatured(b.getIsFeatured())
                .viewCount(b.getViewCount())
                .publishedAt(b.getPublishedAt())
                .createdAt(b.getCreatedAt())
                .build();
    }

    public BlogPostDetailResponse mapToDetailResponse(BlogPost b) {
        List<ProductListResponse> relatedProducts = new ArrayList<>();
        if (b.getRelatedProductIds() != null && !b.getRelatedProductIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(b.getRelatedProductIds());
            Map<UUID, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, Function.identity(), (p1, p2) -> p1));

            relatedProducts = b.getRelatedProductIds().stream()
                    .map(productMap::get)
                    .filter(Objects::nonNull)
                    .map(p -> productService.toListResponse(p, null))
                    .collect(Collectors.toList());
        }

        return BlogPostDetailResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .slug(b.getSlug())
                .excerpt(b.getExcerpt())
                .content(b.getContent())
                .coverImage(b.getCoverImage())
                .category(b.getCategory())
                .tags(b.getTags())
                .authorName(b.getAuthorName())
                .authorImage(b.getAuthorImage())
                .readTimeMinutes(b.getReadTimeMinutes())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .isFeatured(b.getIsFeatured())
                .viewCount(b.getViewCount())
                .publishedAt(b.getPublishedAt())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .metaTitle(b.getMetaTitle())
                .metaDescription(b.getMetaDescription())
                .metaKeywords(b.getMetaKeywords())
                .canonicalUrl(b.getCanonicalUrl())
                .ogImage(b.getOgImage())
                .recommendationType(b.getRecommendationType() != null ? b.getRecommendationType() : "PRODUCTS")
                .relatedCategoryId(b.getRelatedCategoryId())
                .relatedCategoryName(b.getRelatedCategoryName())
                .relatedCategorySlug(b.getRelatedCategorySlug())
                .relatedBrandId(b.getRelatedBrandId())
                .relatedBrandName(b.getRelatedBrandName())
                .relatedBrandSlug(b.getRelatedBrandSlug())
                .relatedManufacturerId(b.getRelatedManufacturerId())
                .relatedManufacturerName(b.getRelatedManufacturerName())
                .relatedProducts(relatedProducts)
                .relatedProductIds(b.getRelatedProductIds() != null ? new ArrayList<>(b.getRelatedProductIds()) : new ArrayList<>())
                .build();
    }
}

