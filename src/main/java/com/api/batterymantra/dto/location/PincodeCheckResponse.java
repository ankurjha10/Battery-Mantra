package com.api.batterymantra.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PincodeCheckResponse {
    private boolean isServiceable;
    private CityDto city;
}
