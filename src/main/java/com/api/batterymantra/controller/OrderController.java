package com.api.batterymantra.controller;

import com.api.batterymantra.dto.order.CheckoutRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.order.OrderStatusUpdateRequest;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // ===== CUSTOMER Endpoints =====

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> checkout(
            @RequestBody @Valid CheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        OrderResponse orderResponse = orderService.placeOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        List<OrderResponse> orderResponse = orderService.getMyOrders(customerId);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        OrderResponse orderResponse = orderService.getOrderById(orderId, customerId);
        return ResponseEntity.ok(orderResponse);
    }

    @DeleteMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userDetails) {
        UUID customerId = userDetails.getUser().getUserId();
        return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId));
    }
}
