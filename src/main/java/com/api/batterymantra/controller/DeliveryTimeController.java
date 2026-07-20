package com.api.batterymantra.controller;

import com.api.batterymantra.dto.DeliveryTimeRequest;
import com.api.batterymantra.dto.DeliveryTimeResponse;
import com.api.batterymantra.service.DeliveryTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeliveryTimeController {

    private final DeliveryTimeService deliveryTimeService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/delivery-time")
    public ResponseEntity<List<DeliveryTimeResponse>> getAllDeliveryTimes() {
        return ResponseEntity.ok(deliveryTimeService.getAllDeliveryTimes());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/delivery-time")
    public ResponseEntity<List<DeliveryTimeResponse>> updateDeliveryTimes(@RequestBody List<DeliveryTimeRequest> requests) {
        return ResponseEntity.ok(deliveryTimeService.updateDeliveryTimes(requests));
    }

    @GetMapping("/public/delivery-time")
    public ResponseEntity<DeliveryTimeResponse> getDeliveryTime(
            @RequestParam UUID categoryId, 
            @RequestParam UUID cityId) {
        return ResponseEntity.ok(deliveryTimeService.getDeliveryTime(categoryId, cityId));
    }
}
