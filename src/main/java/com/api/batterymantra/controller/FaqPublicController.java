package com.api.batterymantra.controller;

import com.api.batterymantra.dto.faq.FaqResponse;
import com.api.batterymantra.enums.FaqPageType;
import com.api.batterymantra.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faqs/public")
@RequiredArgsConstructor
public class FaqPublicController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<List<FaqResponse>> getFaqsByPageType(@RequestParam FaqPageType pageType) {
        return ResponseEntity.ok(faqService.getFaqsByPageType(pageType));
    }
}
