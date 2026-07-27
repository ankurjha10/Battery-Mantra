package com.api.batterymantra.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationResponse {

    private UUID orderId;
    private String orderStatus;
    private String paymentStatus;
    private String message;
}
