package com.api.batterymantra.dto.product;

import com.api.batterymantra.dto.brand.BrandResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAggregationsResponse {
    private List<BrandResponse> brands;
    private List<String> capacities;
    private List<String> warranties;
}
