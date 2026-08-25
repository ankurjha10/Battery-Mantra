package com.api.batterymantra.controller;

import com.api.batterymantra.dto.engineer.EngineerInventoryResponse;
import com.api.batterymantra.dto.engineer.LoadUnloadStockRequest;
import com.api.batterymantra.service.EngineerInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/engineer-inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final EngineerInventoryService inventoryService;

    @PostMapping("/load")
    public ResponseEntity<EngineerInventoryResponse> loadStock(@Valid @RequestBody LoadUnloadStockRequest request) {
        return ResponseEntity.ok(inventoryService.loadStock(request));
    }

    @PostMapping("/unload")
    public ResponseEntity<EngineerInventoryResponse> unloadStock(@Valid @RequestBody LoadUnloadStockRequest request) {
        return ResponseEntity.ok(inventoryService.unloadStock(request));
    }

    @GetMapping("/{engineerId}")
    public ResponseEntity<List<EngineerInventoryResponse>> getEngineerStock(@PathVariable UUID engineerId) {
        return ResponseEntity.ok(inventoryService.getEngineerStock(engineerId));
    }
}
