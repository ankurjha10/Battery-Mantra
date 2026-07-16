package com.api.batterymantra.dto.product;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateProductRequest {
    private String productName;
    private String productDescription;
    private int productStock;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private String productImage;
    private List<String> additionalImages;
    @NotNull(message = "Category is required")
    private UUID categoryId;
    private UUID brandId;
    private Map<String, Object> specs;

    private String capacity;

    @Valid
    private List<CityPricingDto> cityPrices;
}
