package com.api.batterymantra.controller;

import com.api.batterymantra.dto.inventory.CityInventoryBulkUpdateResponse;
import com.api.batterymantra.dto.inventory.PartnerBulkInventoryUpdateRequest;
import com.api.batterymantra.dto.inventory.PartnerInventoryItemResponse;
import com.api.batterymantra.dto.order.OrderNoteRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.product.CityPricingRequestDto;
import com.api.batterymantra.dto.product.CityPricingRequestResponse;
import com.api.batterymantra.dto.user.CreateEngineerRequest;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.service.CityInventoryService;
import com.api.batterymantra.service.CityPricingRequestService;
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
    private final CityPricingRequestService cityPricingRequestService;
    private final CityInventoryService cityInventoryService;

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
            @RequestParam(required = false) String cancellationReason,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        OrderResponse updatedOrder = orderService.updatePartnerOrderStatus(orderId, newStatus, cancellationReason, partnerProfile.getId());
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/orders/{orderId}/notes")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<OrderResponse> addOrderNote(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderNoteRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        OrderResponse updatedOrder = orderService.addPartnerOrderNote(orderId, request.getNote(), partnerProfile.getId());
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

    @PutMapping("/orders/{orderId}/unassign-engineer")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<OrderResponse> unassignEngineer(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        OrderResponse updatedOrder = orderService.unassignEngineerByPartner(orderId, partnerProfile.getId());
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
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        // Verify engineer belongs to this partner before deleting
        EngineerResponse engineer = engineerService.getEngineerById(id);
        if (engineer.getPartnerId() == null || !engineer.getPartnerId().equals(partnerProfile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own engineers");
        }
        engineerService.deleteEngineer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/engineers/available")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<EngineerResponse>> getAvailableEngineers(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(engineerService.getAvailableEngineersByPartner(partnerProfile.getId()));
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

    @PostMapping("/products/{productId}/city-pricing-request")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<CityPricingRequestResponse> createCityPricingRequest(
            @PathVariable UUID productId,
            @Valid @RequestBody CityPricingRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cityPricingRequestService.createPricingRequest(productId, request, partnerProfile));
    }

    @GetMapping("/products/pending-pricing-requests")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<CityPricingRequestResponse>> getPendingPricingRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(cityPricingRequestService.getPendingRequestsForPartner(partnerProfile.getId()));
    }

    // ===== Branch / Multi-City Inventory =====

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<PartnerInventoryItemResponse>> getBranchInventory(
            @RequestParam UUID cityId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(cityInventoryService.getPartnerCityInventory(cityId, partnerProfile));
    }

    @PatchMapping("/inventory/{productId}/stock")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerInventoryItemResponse> updateBranchStock(
            @PathVariable UUID productId,
            @RequestParam UUID cityId,
            @RequestParam int stock,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(cityInventoryService.updatePartnerProductStock(productId, cityId, stock, partnerProfile));
    }

    @PostMapping("/inventory/bulk-update")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<CityInventoryBulkUpdateResponse> bulkUpdateBranchStock(
            @Valid @RequestBody PartnerBulkInventoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PartnerProfile partnerProfile = getPartnerProfile(userPrincipal);
        return ResponseEntity.ok(cityInventoryService.bulkUpdatePartnerCityStock(request, partnerProfile));
    }
}
