package com.api.batterymantra.controller;

import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.user.CreateEngineerRequest;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.service.EngineerService;
import com.api.batterymantra.service.OrderService;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.entity.PartnerProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import com.api.batterymantra.dto.product.CityPricingDto;
import com.api.batterymantra.dto.product.CreateProductRequest;
import com.api.batterymantra.dto.product.ProductDetailResponse;
import com.api.batterymantra.service.ProductService;

import com.api.batterymantra.dto.user.PartnerResponse;
import com.api.batterymantra.service.PartnerService;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerDashboardController {

    private final OrderService orderService;
    private final EngineerService engineerService;
    private final PartnerProfileRepository partnerProfileRepository;
    private final ProductService productService;
    private final PartnerService partnerService;

    private PartnerProfile getPartnerProfile(UserPrincipal userPrincipal) {
        return partnerProfileRepository.findByUserUserId(userPrincipal.getUser().getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner profile not found for current user"));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(partnerService.getPartnerById(partnerProfile.getId()));
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

    @PutMapping("/orders/{orderId}/assign-engineer")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<OrderResponse> assignEngineer(
            @PathVariable UUID orderId,
            @RequestParam UUID engineerId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        OrderResponse updatedOrder = orderService.assignEngineerByPartner(orderId, engineerId, partnerProfile.getId());
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/engineers")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<EngineerResponse>> getMyEngineers(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(engineerService.getEngineersByPartnerId(partnerProfile.getId()));
    }

    @PostMapping("/engineers")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<EngineerResponse> createEngineer(
            @Valid @RequestBody CreateEngineerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(engineerService.createPartnerEngineer(request, partnerProfile.getId()));
    }

    @PutMapping("/engineers/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<EngineerResponse> updateEngineer(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEngineerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        request.setPartnerId(partnerProfile.getId());
        return ResponseEntity.ok(engineerService.updateEngineer(id, request));
    }

    @DeleteMapping("/engineers/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Void> deleteEngineer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        engineerService.deleteEngineer(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Partner Product Request & City Pricing =====

    @PostMapping("/products")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<ProductDetailResponse> requestNewProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addProductByPartner(request, partnerProfile));
    }

    @PutMapping("/products/{productId}/city-pricing")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<ProductDetailResponse> updateCityPricing(
            @PathVariable UUID productId,
            @Valid @RequestBody CityPricingDto dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(productService.updateCityPricingByPartner(productId, dto, partnerProfile));
    }
}
