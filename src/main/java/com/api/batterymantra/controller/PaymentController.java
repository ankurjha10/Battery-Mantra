package com.api.batterymantra.controller;

import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.RazorpayOrderResponse;
import com.api.batterymantra.dto.payment.VerifyPaymentRequest;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/razorpay")
public class PaymentController {

    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RazorpayOrderResponse> createOrder(
            @RequestBody @Valid CreateRazorpayOrderRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        RazorpayOrderResponse response = razorpayService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestBody @Valid VerifyPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        PaymentVerificationResponse response = razorpayService.verifyPayment(customerId, request);
        return ResponseEntity.ok(response);
    }
}
