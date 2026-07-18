package com.api.batterymantra.controller;

import com.api.batterymantra.dto.capacity.CapacityRequest;
import com.api.batterymantra.dto.capacity.CapacityResponse;
import com.api.batterymantra.service.BatteryCapacityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BatteryCapacityController {

    private final BatteryCapacityService capacityService;

    // Public endpoint to get all capacities (optionally filtered by category)
    @GetMapping("/api/capacities")
    public ResponseEntity<List<CapacityResponse>> getAllCapacities(@RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(capacityService.getAllCapacities(categoryId));
    }

    // Admin endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/capacities")
    public ResponseEntity<CapacityResponse> createCapacity(@Valid @RequestBody CapacityRequest request) {
        return new ResponseEntity<>(capacityService.createCapacity(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/capacities/{id}")
    public ResponseEntity<CapacityResponse> updateCapacity(
            @PathVariable UUID id,
            @Valid @RequestBody CapacityRequest request) {
        return ResponseEntity.ok(capacityService.updateCapacity(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/capacities/{id}")
    public ResponseEntity<Void> deleteCapacity(@PathVariable UUID id) {
        capacityService.deleteCapacity(id);
        return ResponseEntity.noContent().build();
    }
}
