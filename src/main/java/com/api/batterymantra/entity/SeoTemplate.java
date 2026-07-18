package com.api.batterymantra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seo_templates")
public class SeoTemplate {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "template_id", updatable = false, nullable = false)
    private UUID templateId;

    @NotBlank
    @Column(name = "template_type", unique = true, nullable = false)
    private String templateType; // e.g. PRODUCT_WITH_CITY, PRODUCT_WITHOUT_CITY, BRAND_WITH_CITY

    @Column(name = "seo_title_template", columnDefinition = "TEXT")
    private String seoTitleTemplate;

    @Column(name = "seo_description_template", columnDefinition = "TEXT")
    private String seoDescriptionTemplate;

    @Column(name = "seo_keywords_template", columnDefinition = "TEXT")
    private String seoKeywordsTemplate;
    
    @Column(name = "og_title_template", columnDefinition = "TEXT")
    private String ogTitleTemplate;
    
    @Column(name = "og_description_template", columnDefinition = "TEXT")
    private String ogDescriptionTemplate;
    
    @Column(name = "short_description_template", columnDefinition = "TEXT")
    private String shortDescriptionTemplate;
}
