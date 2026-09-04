package com.api.batterymantra.dto.product;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.api.batterymantra.entity.SeoMetadata;

@Data
public class CreateProductRequest {
    private String productName;
    private String productDescription;
    private int productStock;
    private BigDecimal productPrice;
    private BigDecimal originalPrice;
    private BigDecimal exchangeDiscount;
    private String productImage;
    private List<String> additionalImages;
    @NotNull(message = "Category is required")
    private UUID categoryId;
    private UUID brandId;
    private List<UUID> specUnitIds;
    private List<UUID> highlightedSpecAttributeIds;
    private Map<UUID, String> specAttributeIcons;

    private String capacity;

    @Valid
    private List<CityPricingDto> cityPrices;

    @JsonProperty("isAutoAssignToPartner")
    private Boolean isAutoAssignToPartner;

    private Integer globalDisplayOrder;
    private Integer categoryDisplayOrder;
    private Integer brandDisplayOrder;
    private Integer categoryBrandDisplayOrder;

    private SeoMetadata seo;
}
