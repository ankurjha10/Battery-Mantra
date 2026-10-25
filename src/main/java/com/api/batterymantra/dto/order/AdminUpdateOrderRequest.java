package com.api.batterymantra.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AdminUpdateOrderRequest {

    // 1. Order Items (replaces items if provided and not empty)
    private List<AdminOrderItemRequest> items;

    // 2. Shipping Address Update
    private UUID addressId;
    private String recipientName;
    private String recipientPhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    // 3. Pricing & Discounts
    private BigDecimal manualDiscount;
    private BigDecimal totalAmount;

    // 4. Delivery & Installation
    private String deliveryMethod; // "HOME_INSTALLATION", "STORE_PICKUP", "STANDARD_DELIVERY"
    private LocalDate installationDate;

    // 5. Payment Details
    private String paymentMethod; // "ONLINE", "COD"
    private String paymentStatus; // "PENDING", "PAID", "FAILED", "REFUNDED"
    private String paymentMode;   // "UPI", "CARD", "NET_BANKING", "WALLET", "CASH"

    // 6. Notes & City
    private String engineerNotes;
    private String selectedCity;
}
