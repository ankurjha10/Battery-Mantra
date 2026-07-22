package com.api.batterymantra.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID productId;

    @NotBlank
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String productDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id", nullable = false)
    @NotNull
    private Category productCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> spec = new HashMap<>();

    private String capacity;

    @Min(0)
    private int productStock;

    @PositiveOrZero
    private BigDecimal productPrice;

    @Min(0)
    private BigDecimal exchangeDiscount = BigDecimal.ZERO;

    @NotBlank
    private String productImage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_additional_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> additionalImages = new ArrayList<>();

    @CreationTimestamp
    private LocalDate productDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCityPricing> cityPrices = new ArrayList<>();

    // Assignment flag
    @Column(name = "is_auto_assign_to_partner", nullable = false)
    private boolean isAutoAssignToPartner = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Embedded
    private SeoMetadata seo = new SeoMetadata();
}
