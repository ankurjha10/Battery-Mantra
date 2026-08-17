package com.api.batterymantra.controller;

import com.api.batterymantra.dto.spec.*;
import com.api.batterymantra.service.SpecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/specs")
@RequiredArgsConstructor
public class AdminSpecController {

    private final SpecService specService;

    // ======================== Spec Categories ========================

    @GetMapping("/categories")
    public ResponseEntity<List<SpecCategoryResponse>> getSpecCategories(@RequestParam UUID categoryId) {
        return ResponseEntity.ok(specService.getSpecCategoriesByCategoryId(categoryId));
    }

    @PostMapping("/categories")
    public ResponseEntity<SpecCategoryResponse> createSpecCategory(@RequestBody @Valid SpecCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.createSpecCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<SpecCategoryResponse> updateSpecCategory(@PathVariable UUID id, @RequestBody @Valid SpecCategoryRequest request) {
        return ResponseEntity.ok(specService.updateSpecCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteSpecCategory(@PathVariable UUID id) {
        specService.deleteSpecCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== Spec Attributes ========================

    @GetMapping("/attributes")
    public ResponseEntity<List<SpecAttributeResponse>> getSpecAttributes(@RequestParam UUID specCategoryId) {
        return ResponseEntity.ok(specService.getSpecAttributesBySpecCategoryId(specCategoryId));
    }

    @PostMapping("/attributes")
    public ResponseEntity<SpecAttributeResponse> createSpecAttribute(@RequestBody @Valid SpecAttributeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.createSpecAttribute(request));
    }

    @PutMapping("/attributes/{id}")
    public ResponseEntity<SpecAttributeResponse> updateSpecAttribute(@PathVariable UUID id, @RequestBody @Valid SpecAttributeRequest request) {
        return ResponseEntity.ok(specService.updateSpecAttribute(id, request));
    }

    @DeleteMapping("/attributes/{id}")
    public ResponseEntity<Void> deleteSpecAttribute(@PathVariable UUID id) {
        specService.deleteSpecAttribute(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== Spec Units ========================

    @GetMapping("/units")
    public ResponseEntity<List<SpecUnitResponse>> getSpecUnits(@RequestParam UUID attributeId) {
        return ResponseEntity.ok(specService.getSpecUnitsByAttributeId(attributeId));
    }

    @PostMapping("/units")
    public ResponseEntity<SpecUnitResponse> createSpecUnit(@RequestBody @Valid SpecUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.createSpecUnit(request));
    }

    @PutMapping("/units/{id}")
    public ResponseEntity<SpecUnitResponse> updateSpecUnit(@PathVariable UUID id, @RequestBody @Valid SpecUnitRequest request) {
        return ResponseEntity.ok(specService.updateSpecUnit(id, request));
    }

    @DeleteMapping("/units/{id}")
    public ResponseEntity<Void> deleteSpecUnit(@PathVariable UUID id) {
        specService.deleteSpecUnit(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== Category Spec Template ========================

    @GetMapping("/template/category/{categoryId}")
    public ResponseEntity<CategorySpecTemplateResponse> getCategorySpecTemplate(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(specService.getCategorySpecTemplate(categoryId));
    }
}
