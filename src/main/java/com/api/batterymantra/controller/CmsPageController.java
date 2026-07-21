package com.api.batterymantra.controller;

import com.api.batterymantra.dto.cms.CmsPageDto;
import com.api.batterymantra.dto.cms.CreateCmsPageRequest;
import com.api.batterymantra.service.CmsPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CmsPageController {

    private final CmsPageService cmsPageService;

    // Public Endpoint
    @GetMapping("/api/cms/pages/{slug}")
    public ResponseEntity<CmsPageDto> getPageBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(cmsPageService.getPageBySlug(slug));
    }

    // Admin Endpoints
    @GetMapping("/api/admin/cms/pages")
    public ResponseEntity<List<CmsPageDto>> getAllPages() {
        return ResponseEntity.ok(cmsPageService.getAllPages());
    }

    @GetMapping("/api/admin/cms/pages/{id}")
    public ResponseEntity<CmsPageDto> getPageById(@PathVariable UUID id) {
        return ResponseEntity.ok(cmsPageService.getPageById(id));
    }

    @PostMapping("/api/admin/cms/pages")
    public ResponseEntity<CmsPageDto> createPage(@Valid @RequestBody CreateCmsPageRequest request) {
        return new ResponseEntity<>(cmsPageService.createPage(request), HttpStatus.CREATED);
    }

    @PutMapping("/api/admin/cms/pages/{id}")
    public ResponseEntity<CmsPageDto> updatePage(@PathVariable UUID id, @Valid @RequestBody CreateCmsPageRequest request) {
        return ResponseEntity.ok(cmsPageService.updatePage(id, request));
    }

    @DeleteMapping("/api/admin/cms/pages/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable UUID id) {
        cmsPageService.deletePage(id);
        return ResponseEntity.noContent().build();
    }
}
