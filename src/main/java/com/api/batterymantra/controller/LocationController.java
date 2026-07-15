package com.api.batterymantra.controller;

import com.api.batterymantra.dto.location.CityDto;
import com.api.batterymantra.dto.location.PincodeCheckResponse;
import com.api.batterymantra.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getAllCities() {
        return ResponseEntity.ok(locationService.getAllCities());
    }

    @GetMapping("/check-pincode")
    public ResponseEntity<PincodeCheckResponse> checkPincode(@RequestParam String code) {
        return ResponseEntity.ok(locationService.checkPincode(code));
    }
}
