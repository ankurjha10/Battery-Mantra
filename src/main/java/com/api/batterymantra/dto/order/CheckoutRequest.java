package com.api.batterymantra.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CheckoutRequest {

    @NotNull(message = "Select an Address")
    private UUID addressId;

    @NotNull(message = "Select a Delivery Method")
    private String deliveryMethod;

    private String paymentMethod;

    private LocalDate installationDate;
}
