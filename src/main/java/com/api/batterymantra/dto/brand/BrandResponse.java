package com.api.batterymantra.dto.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private UUID brandId;
    private String brandName;
    private String brandLogo;
    private boolean featured;
}
