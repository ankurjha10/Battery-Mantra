package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cms_pages")
public class CmsPage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "page_id", updatable = false, nullable = false)
    private UUID pageId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "sub_title")
    private String subTitle;

    @Column(name = "image1")
    private String image1;

    @Column(name = "image2")
    private String image2;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "content2", columnDefinition = "TEXT")
    private String content2;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Embedded
    private SeoMetadata seo = new SeoMetadata();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
