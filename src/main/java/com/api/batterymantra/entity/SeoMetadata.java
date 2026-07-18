package com.api.batterymantra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeoMetadata {

    @Column(name = "seo_slug", unique = true)
    private String slug;

    @Column(name = "seo_meta_title")
    private String metaTitle;

    @Column(name = "seo_meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "seo_meta_keywords", columnDefinition = "TEXT")
    private String metaKeywords;

    @Column(name = "seo_meta_title_city")
    private String metaTitleCity;

    @Column(name = "seo_meta_description_city", columnDefinition = "TEXT")
    private String metaDescriptionCity;

    @Column(name = "seo_meta_keywords_city", columnDefinition = "TEXT")
    private String metaKeywordsCity;

    @Column(name = "seo_og_title")
    private String ogTitle;

    @Column(name = "seo_og_description", columnDefinition = "TEXT")
    private String ogDescription;

    @Column(name = "seo_og_title_city")
    private String ogTitleCity;

    @Column(name = "seo_og_description_city", columnDefinition = "TEXT")
    private String ogDescriptionCity;

    @Column(name = "seo_canonical_url")
    private String canonicalUrl;
}
