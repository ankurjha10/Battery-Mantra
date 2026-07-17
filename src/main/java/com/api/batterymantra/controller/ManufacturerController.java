package com.api.batterymantra.controller;

import com.api.batterymantra.dto.manufacturer.CreateManufacturerRequest;
import com.api.batterymantra.dto.manufacturer.ManufacturerResponse;
import com.api.batterymantra.dto.manufacturer.UpdateManufacturerRequest;
import com.api.batterymantra.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public ResponseEntity<List<ManufacturerResponse>> getAllManufacturers() {
        return ResponseEntity.ok(manufacturerService.getAllManufacturers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManufacturerResponse> getManufacturerById(@PathVariable UUID id) {
        return ResponseEntity.ok(manufacturerService.getManufacturerById(id));
    }

    @PostMapping
    public ResponseEntity<ManufacturerResponse> createManufacturer(@Valid @RequestBody CreateManufacturerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manufacturerService.createManufacturer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManufacturerResponse> updateManufacturer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManufacturerRequest request) {
        return ResponseEntity.ok(manufacturerService.updateManufacturer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManufacturer(@PathVariable UUID id) {
        manufacturerService.deleteManufacturer(id);
        return ResponseEntity.noContent().build();
    }
}
