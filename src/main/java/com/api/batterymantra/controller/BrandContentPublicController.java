package com.api.batterymantra.controller;

import com.api.batterymantra.dto.brandcontent.BrandContentResponse;
import com.api.batterymantra.service.BrandContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/brand-contents")
@RequiredArgsConstructor
public class BrandContentPublicController {

    private final BrandContentService brandContentService;

    @GetMapping
    public ResponseEntity<BrandContentResponse> getBrandContent(
            @RequestParam UUID categoryId,
            @RequestParam UUID brandId
    ) {
        BrandContentResponse response = brandContentService.getByCategoryIdAndBrandId(categoryId, brandId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }
}
