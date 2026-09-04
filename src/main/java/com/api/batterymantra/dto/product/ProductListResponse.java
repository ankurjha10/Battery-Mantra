package com.api.batterymantra.dto.product;

import com.api.batterymantra.entity.SeoMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductListResponse {
    private UUID productId;
    private String productName;
    private String brandName;
    private BigDecimal productPrice;
    private BigDecimal originalPrice;
    private BigDecimal exchangeDiscount;
    private String productImage;
    private List<String> additionalImages;
    private String productCategory;
    private String capacity;
    @JsonProperty("isAutoAssignToPartner")
    private boolean isAutoAssignToPartner;
    @JsonProperty("isApproved")
    private boolean isApproved = true;
    private UUID createdByPartnerId;
    private String partnerBusinessName;
    private Integer globalDisplayOrder;
    private Integer categoryDisplayOrder;
    private Integer brandDisplayOrder;
    private String highlights;
    private SeoMetadata seo;
}
