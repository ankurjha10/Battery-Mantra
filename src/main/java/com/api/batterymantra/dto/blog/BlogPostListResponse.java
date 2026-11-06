package com.api.batterymantra.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostListResponse {

    private UUID id;
    private String title;
    private String slug;
    private String excerpt;
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
}

