package com.api.batterymantra.dto.brand;

import lombok.Data;

@Data
public class BrandRequest {

    private String brandName;
    private String brandLogo;
    private boolean featured;
}
