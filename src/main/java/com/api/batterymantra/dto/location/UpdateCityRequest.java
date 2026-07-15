package com.api.batterymantra.dto.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCityRequest {
    private String cityName;
    private String stateName;
    private String cityImage; // Optional
    private Boolean isPopular;
    private Boolean isCodAvailable;
    private Boolean isExchangeAvailable;
}
