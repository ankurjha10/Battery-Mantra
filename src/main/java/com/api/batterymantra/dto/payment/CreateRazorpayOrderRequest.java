package com.api.batterymantra.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRazorpayOrderRequest {

    @NotNull(message = "Select an Address")
    private UUID addressId;

    @NotNull(message = "Select a Delivery Method")
    private String deliveryMethod;

    private LocalDate installationDate;

    private String selectedCity;

    private String couponCode;

    private UUID appliedCampaignId;
}
