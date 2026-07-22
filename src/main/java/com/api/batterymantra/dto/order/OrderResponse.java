package com.api.batterymantra.dto.order;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private String orderStatus;
    private String shippingAddress;
    private LocalDateTime placedAt;
    private BigDecimal totalAmount;
    private String deliveryMethod;
    private String paymentMethod;
    private LocalDate installationDate;
    private BigDecimal exchangeDiscount;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private List<OrderItemResponse> orderItems;
    private com.api.batterymantra.dto.user.PartnerResponse assignedPartner;
    private com.api.batterymantra.dto.user.EngineerResponse assignedEngineer;
}
