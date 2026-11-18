package com.api.batterymantra.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedRequest {

    @NotBlank(message = "Razorpay Order ID is required")
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String reason;
}
