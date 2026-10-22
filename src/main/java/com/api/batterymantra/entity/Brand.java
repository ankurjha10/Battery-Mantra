package com.api.batterymantra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID brandId;

    @NotBlank
    private String brandName;

    private String brandLogo;

    @Column(name = "is_featured")
    private boolean featured;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dealer_description", columnDefinition = "TEXT")
    private String dealerDescription;


    @Column(name = "slug")
    private String slug;

    @Embedded
    private SeoMetadata seo = new SeoMetadata();

    public String getSlug() {
        if (slug != null && !slug.trim().isEmpty()) {
            return slug;
        }
        if (seo != null && seo.getSlug() != null && !seo.getSlug().trim().isEmpty()) {
            return seo.getSlug();
        }
        return brandName != null ? brandName.toLowerCase().replace(" ", "-") : null;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @org.hibernate.annotations.Formula("(select cast(count(*) as int) from products p where p.brand_id = brand_id and p.is_approved = true)")
    private Integer productCount;
}
