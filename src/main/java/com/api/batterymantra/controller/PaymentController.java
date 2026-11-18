package com.api.batterymantra.controller;

import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentFailedRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.QrCodeResponse;
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

import java.util.Map;
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

    /**
     * Called by frontend when Razorpay reports payment.failed or user dismisses the payment modal.
     * Records failure on the pending checkout session; no order is created.
     */
    @PostMapping("/failed")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> markPaymentFailed(
            @RequestBody @Valid PaymentFailedRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        Map<String, Object> response = razorpayService.markPaymentFailed(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Public Razorpay server-to-server webhook endpoint.
     * Authenticated exclusively via HMAC-SHA256 X-Razorpay-Signature header.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        Map<String, Object> response = razorpayService.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate a Razorpay Dynamic UPI QR Code for an existing (COD) order.
     * POST /api/payments/razorpay/generate-qr/{orderId}
     */
    @PostMapping("/generate-qr/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ENGINEER')")
    public ResponseEntity<QrCodeResponse> generateQrCode(
            @PathVariable UUID orderId) {
        QrCodeResponse response = razorpayService.generateQrCode(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Poll the payment status of a QR code for the given order.
     * GET /api/payments/razorpay/status/{orderId}
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ENGINEER')")
    public ResponseEntity<Map<String, Object>> checkQrPaymentStatus(
            @PathVariable UUID orderId) {
        Map<String, Object> status = razorpayService.checkQrPaymentStatus(orderId);
        return ResponseEntity.ok(status);
    }
}
