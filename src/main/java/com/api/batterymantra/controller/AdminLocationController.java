package com.api.batterymantra.controller;

import com.api.batterymantra.dto.location.*;
import com.api.batterymantra.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminLocationController {

    private final LocationService locationService;

    // --- Cities ---
    
    @PostMapping("/cities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityDto> createCity(@RequestBody @Valid CreateCityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createCity(request));
    }

    @GetMapping("/cities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CityDto>> getAllCities() {
        return ResponseEntity.ok(locationService.getAllCities());
    }

    @PutMapping("/cities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityDto> updateCity(@PathVariable UUID id, @RequestBody @Valid UpdateCityRequest request) {
        return ResponseEntity.ok(locationService.updateCity(id, request));
    }

    @DeleteMapping("/cities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCity(@PathVariable UUID id) {
        locationService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }

    // --- Pincodes ---

    @PostMapping("/cities/{cityId}/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PincodeDto>> addPincodes(
            @PathVariable UUID cityId, 
            @RequestBody @Valid AddPincodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.addPincodes(cityId, request));
    }

    @GetMapping("/cities/{cityId}/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PincodeDto>> getPincodesByCity(@PathVariable UUID cityId) {
        return ResponseEntity.ok(locationService.getPincodesByCity(cityId));
    }

    @DeleteMapping("/pincodes/{pincodeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePincode(@PathVariable UUID pincodeId) {
        locationService.deletePincode(pincodeId);
        return ResponseEntity.noContent().build();
    }
}
