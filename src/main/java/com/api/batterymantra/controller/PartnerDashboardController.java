package com.api.batterymantra.controller;

import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.service.OrderService;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.entity.PartnerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerDashboardController {

    private final OrderService orderService;
    private final PartnerProfileRepository partnerProfileRepository;

    private PartnerProfile getPartnerProfile(UserPrincipal userPrincipal) {
        return partnerProfileRepository.findByUserUserId(userPrincipal.getUser().getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner profile not found for current user"));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<OrderResponse>> getAssignedOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        List<OrderResponse> orders = orderService.getPartnerOrders(partnerProfile.getId());
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus newStatus,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        OrderResponse updatedOrder = orderService.updatePartnerOrderStatus(orderId, newStatus, partnerProfile.getId());
        return ResponseEntity.ok(updatedOrder);
    }
}
