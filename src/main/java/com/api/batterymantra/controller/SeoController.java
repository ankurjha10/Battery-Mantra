package com.api.batterymantra.controller;

import com.api.batterymantra.entity.SeoPage;
import com.api.batterymantra.entity.SeoTemplate;
import com.api.batterymantra.service.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seo")
@RequiredArgsConstructor
public class SeoController {

    private final SeoService seoService;

    // --- Seo Pages ---
    
    @GetMapping("/pages")
    public ResponseEntity<List<SeoPage>> getAllPages() {
        return ResponseEntity.ok(seoService.getAllSeoPages());
    }

    @GetMapping("/pages/route")
    public ResponseEntity<SeoPage> getPageByRoute(@RequestParam String route) {
        SeoPage page = seoService.getSeoPageByRoute(route);
        return page != null ? ResponseEntity.ok(page) : ResponseEntity.notFound().build();
    }

    @PostMapping("/pages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeoPage> createPage(@RequestBody SeoPage seoPage) {
        return ResponseEntity.ok(seoService.saveSeoPage(seoPage));
    }

    @PutMapping("/pages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeoPage> updatePage(@PathVariable UUID id, @RequestBody SeoPage seoPage) {
        seoPage.setPageId(id);
        return ResponseEntity.ok(seoService.saveSeoPage(seoPage));
    }

    @DeleteMapping("/pages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePage(@PathVariable UUID id) {
        seoService.deleteSeoPage(id);
        return ResponseEntity.ok().build();
    }

    // --- Seo Templates (Quick SEO) ---

    @GetMapping("/templates")
    public ResponseEntity<List<SeoTemplate>> getAllTemplates() {
        return ResponseEntity.ok(seoService.getAllSeoTemplates());
    }

    @GetMapping("/templates/{type}")
    public ResponseEntity<SeoTemplate> getTemplateByType(@PathVariable String type) {
        SeoTemplate template = seoService.getSeoTemplateByType(type);
        return template != null ? ResponseEntity.ok(template) : ResponseEntity.notFound().build();
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeoTemplate> saveTemplate(@RequestBody SeoTemplate seoTemplate) {
        return ResponseEntity.ok(seoService.saveSeoTemplate(seoTemplate));
    }
}
