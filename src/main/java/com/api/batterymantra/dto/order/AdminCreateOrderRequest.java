package com.api.batterymantra.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AdminCreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    private UUID addressId; // Can be null if the customer only has one address or default should be used

    private BigDecimal discount = BigDecimal.ZERO; // Manual admin discount

    @NotEmpty(message = "Order items cannot be empty")
    private List<AdminOrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // "ONLINE" or "COD"

    @NotNull(message = "Delivery method is required")
    private String deliveryMethod; // "HOME_INSTALLATION", "STORE_PICKUP", "STANDARD_DELIVERY"

    private LocalDate installationDate;
}
