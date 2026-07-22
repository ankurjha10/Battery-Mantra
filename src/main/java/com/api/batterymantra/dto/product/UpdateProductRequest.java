package com.api.batterymantra.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateProductRequest {
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private UUID categoryId;
    private UUID brandId;
    private String productImage;
    private List<String> additionalImages;
    private Integer productStock;
    private Map<String, Object> specs;
    private String capacity;

    @jakarta.validation.Valid
    private List<CityPricingDto> cityPrices;

    @com.fasterxml.jackson.annotation.JsonProperty("isAutoAssignToPartner")
    private Boolean isAutoAssignToPartner;
}
