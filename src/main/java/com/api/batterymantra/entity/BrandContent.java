package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "brand_contents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_contents_category_brand", columnNames = {"category_id", "brand_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "normal_short_description", columnDefinition = "TEXT")
    private String normalShortDescription;

    @Column(name = "normal_description", columnDefinition = "TEXT")
    private String normalDescription;

    @Column(name = "normal_seo_title")
    private String normalSeoTitle;

    @Column(name = "dealer_short_description", columnDefinition = "TEXT")
    private String dealerShortDescription;

    @Column(name = "dealer_description", columnDefinition = "TEXT")
    private String dealerDescription;

    @Column(name = "dealer_seo_title")
    private String dealerSeoTitle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
