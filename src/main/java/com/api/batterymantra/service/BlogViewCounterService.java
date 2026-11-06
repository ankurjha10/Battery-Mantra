package com.api.batterymantra.service;

import com.api.batterymantra.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogViewCounterService {

    private final BlogPostRepository blogPostRepository;

    @Async
    @Transactional
    public void incrementViewCount(UUID blogId) {
        if (blogId == null) {
            return;
        }
        try {
            blogPostRepository.incrementViewCount(blogId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for blog post {}: {}", blogId, e.getMessage());
        }
    }
}

