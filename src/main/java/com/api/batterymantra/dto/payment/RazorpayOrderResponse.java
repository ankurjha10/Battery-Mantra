package com.api.batterymantra.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RazorpayOrderResponse {

    private String razorpayOrderId;
    private long amount;
    private String currency;
    private String keyId;
    private UUID orderId;
}
