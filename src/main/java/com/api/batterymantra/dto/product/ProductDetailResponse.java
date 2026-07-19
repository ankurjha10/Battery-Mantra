package com.api.batterymantra.dto.product;

import com.api.batterymantra.dto.vehicle.VehicleResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ProductDetailResponse {
    private UUID productId;
    private String productName;
    private String productDescription;
    private String brandName;
    private UUID brandId;
    private String categoryName;
    private UUID categoryId;
    private BigDecimal productPrice;
    private BigDecimal exchangeDiscount;
    private int productStock;
    private String productImage;
    private List<String> additionalImages;
    private Map<String, Object> specs;
    private String capacity;
    private List<CityPricingDto> cityPrices;
    private com.api.batterymantra.entity.SeoMetadata seo;
}
