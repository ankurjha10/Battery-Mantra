package com.api.batterymantra.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateRazorpayOrderRequest {

    @NotNull(message = "Select an Address")
    private UUID addressId;

    @NotNull(message = "Select a Delivery Method")
    private String deliveryMethod;

    private LocalDate installationDate;
}
