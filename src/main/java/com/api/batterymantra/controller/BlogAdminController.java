package com.api.batterymantra.controller;

import com.api.batterymantra.dto.blog.*;
import com.api.batterymantra.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN')")
@Tag(name = "Blog Admin", description = "Admin Blog Management Endpoints")
public class BlogAdminController {

    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "Get all blog posts (DRAFT and PUBLISHED) for admin data table")
    public ResponseEntity<Page<BlogPostListResponse>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        Page<BlogPostListResponse> blogs = blogService.getAdminBlogs(page, size, category, status, keyword);
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blog post detail by UUID for admin edit form")
    public ResponseEntity<BlogPostDetailResponse> getBlogById(@PathVariable UUID id) {
        return ResponseEntity.ok(blogService.getAdminBlogById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new blog post with automatic slugification and read time calculation")
    public ResponseEntity<BlogPostDetailResponse> createBlog(@Valid @RequestBody CreateBlogPostRequest request) {
        BlogPostDetailResponse created = blogService.createBlog(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing blog post")
    public ResponseEntity<BlogPostDetailResponse> updateBlog(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBlogPostRequest request
    ) {
        return ResponseEntity.ok(blogService.updateBlog(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Quickly toggle blog publish status (DRAFT or PUBLISHED)")
    public ResponseEntity<BlogPostDetailResponse> updateBlogStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBlogStatusRequest request
    ) {
        return ResponseEntity.ok(blogService.updateBlogStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a blog post")
    public ResponseEntity<Void> deleteBlog(@PathVariable UUID id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }
}

