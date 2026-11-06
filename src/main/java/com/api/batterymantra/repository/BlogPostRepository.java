package com.api.batterymantra.repository;

import com.api.batterymantra.dto.blog.BlogCategoryCountResponse;
import com.api.batterymantra.entity.BlogPost;
import com.api.batterymantra.entity.enums.BlogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID>, JpaSpecificationExecutor<BlogPost> {

    Optional<BlogPost> findBySlugAndStatus(String slug, BlogStatus status);

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<BlogPost> findTop3ByIsFeaturedTrueAndStatusOrderByPublishedAtDescCreatedAtDesc(BlogStatus status);

    @Query("SELECT new com.api.batterymantra.dto.blog.BlogCategoryCountResponse(b.category, COUNT(b)) " +
           "FROM BlogPost b WHERE b.status = :status GROUP BY b.category ORDER BY COUNT(b) DESC")
    List<BlogCategoryCountResponse> countPostsByCategory(@Param("status") BlogStatus status);

    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}

