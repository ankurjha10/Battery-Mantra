package com.api.batterymantra.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityDto {
    private UUID cityId;
    private String cityName;
    private String stateName;
    private String cityImage;
    private Boolean isPopular;
    private Boolean isCodAvailable;
    private Boolean isExchangeAvailable;
    private int pincodeCount;
}
