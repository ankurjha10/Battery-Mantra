package com.api.batterymantra.controller;

import com.api.batterymantra.dto.BulkPricingRequest;
import com.api.batterymantra.dto.BulkPricingResponse;
import com.api.batterymantra.service.BulkPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bulk-pricing")
@RequiredArgsConstructor
public class BulkPricingController {

    private final BulkPricingService bulkPricingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BulkPricingResponse>> getMatrix(
            @RequestParam UUID categoryId,
            @RequestParam UUID brandId) {
        return ResponseEntity.ok(bulkPricingService.getMatrix(categoryId, brandId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkPricingResponse> updateMatrix(@RequestBody BulkPricingRequest request) {
        return ResponseEntity.ok(bulkPricingService.updateMatrix(request));
    }
}
