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
@Table(name = "seo_pages")
public class SeoPage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "page_id", updatable = false, nullable = false)
    private UUID pageId;

    @NotBlank
    @Column(name = "page_route", unique = true, nullable = false)
    private String pageRoute;

    @NotBlank
    @Column(name = "page_name", nullable = false)
    private String pageName;

    @Embedded
    private SeoMetadata seo = new SeoMetadata();
}
