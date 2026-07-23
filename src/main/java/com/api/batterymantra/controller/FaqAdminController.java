package com.api.batterymantra.controller;

import com.api.batterymantra.dto.faq.FaqRequest;
import com.api.batterymantra.dto.faq.FaqResponse;
import com.api.batterymantra.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FaqAdminController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<List<FaqResponse>> getAllFaqs() {
        return ResponseEntity.ok(faqService.getAllFaqs());
    }

    @PostMapping
    public ResponseEntity<FaqResponse> createFaq(@Valid @RequestBody FaqRequest request) {
        return new ResponseEntity<>(faqService.createFaq(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FaqResponse> updateFaq(@PathVariable UUID id, @Valid @RequestBody FaqRequest request) {
        return ResponseEntity.ok(faqService.updateFaq(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable UUID id) {
        faqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
}
