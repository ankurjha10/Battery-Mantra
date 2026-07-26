package com.api.batterymantra.dto.location;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCityRequest {
    @NotBlank(message = "City name is required")
    private String cityName;

    @NotBlank(message = "State name is required")
    private String stateName;

    private String cityImage; // Optional
    private Boolean isPopular = false;
    private Boolean isCodAvailable = false;
    private Boolean isExchangeAvailable = false;
    private Double deliveryCharge = 0.0;
    private Double originalDeliveryCharge = 40.0;
}
