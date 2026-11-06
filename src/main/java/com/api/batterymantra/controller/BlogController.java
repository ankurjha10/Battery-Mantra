package com.api.batterymantra.controller;

import com.api.batterymantra.dto.blog.BlogCategoryCountResponse;
import com.api.batterymantra.dto.blog.BlogPostDetailResponse;
import com.api.batterymantra.dto.blog.BlogPostListResponse;
import com.api.batterymantra.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blog Public", description = "Public Blog and Content Endpoints for Organic SEO & Traffic")
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "Get published blog posts with filtering and pagination")
    public ResponseEntity<Page<BlogPostListResponse>> getPublishedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean isFeatured
    ) {
        Page<BlogPostListResponse> result = blogService.getPublicBlogs(
                page,
                size,
                category,
                keyword,
                tag,
                isFeatured
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get published blog post detail by slug (increments view count and hydrates related products)")
    public ResponseEntity<BlogPostDetailResponse> getBlogBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getPublicBlogBySlug(slug));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get blog categories with published post count")
    public ResponseEntity<List<BlogCategoryCountResponse>> getCategories() {
        return ResponseEntity.ok(blogService.getCategoriesWithCounts());
    }

    @GetMapping("/featured")
    @Operation(summary = "Get top featured published blog posts for hero banner section")
    public ResponseEntity<List<BlogPostListResponse>> getFeaturedBlogs() {
        return ResponseEntity.ok(blogService.getFeaturedBlogs());
    }
}

