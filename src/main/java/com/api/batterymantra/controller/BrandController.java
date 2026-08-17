package com.api.batterymantra.controller;

import com.api.batterymantra.dto.brand.BrandResponse;
import com.api.batterymantra.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getAllBrands(@RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(brandService.getAllBrands(categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(@PathVariable UUID id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<BrandResponse>> getFeaturedBrands() {
        return ResponseEntity.ok(brandService.getFeaturedBrands());
    }
}
