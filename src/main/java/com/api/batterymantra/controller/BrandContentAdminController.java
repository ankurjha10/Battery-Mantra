package com.api.batterymantra.controller;

import com.api.batterymantra.dto.brandcontent.BrandContentRequest;
import com.api.batterymantra.dto.brandcontent.BrandContentResponse;
import com.api.batterymantra.service.BrandContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/brand-contents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN')")
public class BrandContentAdminController {

    private final BrandContentService brandContentService;

    @GetMapping
    public ResponseEntity<List<BrandContentResponse>> getAll() {
        return ResponseEntity.ok(brandContentService.getAll());
    }

    @GetMapping("/find")
    public ResponseEntity<BrandContentResponse> getByPair(
            @RequestParam UUID categoryId,
            @RequestParam UUID brandId
    ) {
        BrandContentResponse response = brandContentService.getByCategoryIdAndBrandId(categoryId, brandId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<BrandContentResponse> saveOrUpdate(@Valid @RequestBody BrandContentRequest request) {
        return ResponseEntity.ok(brandContentService.saveOrUpdate(request));
    }
}
