package com.api.batterymantra.controller.admin;

import com.api.batterymantra.dto.inventory.*;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.StockStatus;
import com.api.batterymantra.service.CentralInventoryService;
import com.api.batterymantra.service.CityInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN')")
public class CentralInventoryController {

    private final CentralInventoryService inventoryService;
    private final CityInventoryService cityInventoryService;

    /**
     * GET /api/admin/inventory
     * Dashboard: Summary metrics + paginated inventory items with status filtering.
     */
    @GetMapping
    public ResponseEntity<InventoryDashboardResponse> getInventory(
            @RequestParam(required = false, defaultValue = "ALL") StockStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        InventoryDashboardResponse response = inventoryService.getInventoryDashboard(
                status, categoryId, brandId, search, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/admin/inventory/{productId}/stock
     * Inline stock quantity adjustment for a single product.
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<UpdateStockResponse> updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String performedBy = principal != null && principal.getUser() != null && principal.getUsername() != null
                ? principal.getUsername()
                : "ADMIN";

        UpdateStockResponse response = inventoryService.updateProductStock(productId, request, performedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/inventory/bulk-update
     * Bulk stock quantity updater across multiple products.
     */
    @PutMapping("/bulk-update")
    public ResponseEntity<BulkStockUpdateResponse> bulkUpdateStock(
            @Valid @RequestBody List<BulkStockUpdateRequest> requests,
            @AuthenticationPrincipal UserPrincipal principal) {

        String performedBy = principal != null && principal.getUser() != null && principal.getUsername() != null
                ? principal.getUsername()
                : "ADMIN";

        BulkStockUpdateResponse response = inventoryService.bulkUpdateStock(requests, performedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/inventory/{productId}/logs
     * View history / audit trail of stock changes for a product.
     */
    @GetMapping("/{productId}/logs")
    public ResponseEntity<Page<StockAuditLogResponse>> getProductAuditLogs(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<StockAuditLogResponse> logs = inventoryService.getProductAuditLogs(
                productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(logs);
    }

    // ===== Admin Multi-City Inventory =====

    /**
     * GET /api/admin/inventory/cities/{cityId}
     * Unrestricted view of any city's branch inventory for Super Admin.
     */
    @GetMapping("/cities/{cityId}")
    public ResponseEntity<List<PartnerInventoryItemResponse>> getCityInventory(
            @PathVariable UUID cityId) {
        return ResponseEntity.ok(cityInventoryService.getCityInventoryForAdmin(cityId));
    }

    /**
     * PATCH /api/admin/inventory/cities/{cityId}/products/{productId}
     * Updates stock in the city_pricing table for that product & city.
     */
    @PatchMapping("/cities/{cityId}/products/{productId}")
    public ResponseEntity<PartnerInventoryItemResponse> updateCityProductStock(
            @PathVariable UUID cityId,
            @PathVariable UUID productId,
            @Valid @RequestBody CityStockUpdateRequest request) {
        return ResponseEntity.ok(cityInventoryService.updateAdminCityProductStock(cityId, productId, request.getStock()));
    }

    /**
     * PUT /api/admin/inventory/cities/{cityId}/bulk-update
     * Bulk updates stock in the city_pricing table for that city.
     */
    @PutMapping("/cities/{cityId}/bulk-update")
    public ResponseEntity<CityInventoryBulkUpdateResponse> bulkUpdateCityStock(
            @PathVariable UUID cityId,
            @Valid @RequestBody List<CityProductStockItem> updates) {
        return ResponseEntity.ok(cityInventoryService.bulkUpdateAdminCityStock(cityId, updates));
    }
}

