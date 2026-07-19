package com.api.batterymantra.controller;

import com.api.batterymantra.entity.Fuel;
import com.api.batterymantra.service.FuelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fuels")
@RequiredArgsConstructor
public class FuelController {

    private final FuelService fuelService;

    @GetMapping
    public ResponseEntity<List<Fuel>> getAllFuels() {
        return ResponseEntity.ok(fuelService.getAllFuels());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Fuel> createFuel(@RequestBody Fuel fuel) {
        return ResponseEntity.ok(fuelService.createFuel(fuel));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Fuel> updateFuel(@PathVariable UUID id, @RequestBody Fuel fuel) {
        return ResponseEntity.ok(fuelService.updateFuel(id, fuel));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFuel(@PathVariable UUID id) {
        fuelService.deleteFuel(id);
        return ResponseEntity.ok().build();
    }
}
