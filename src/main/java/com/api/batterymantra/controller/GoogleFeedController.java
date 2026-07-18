package com.api.batterymantra.controller;

import com.api.batterymantra.service.GoogleFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seo")
@RequiredArgsConstructor
public class GoogleFeedController {

    private final GoogleFeedService googleFeedService;

    @GetMapping(value = "/google-feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getGoogleProductsFeed() {
        return ResponseEntity.ok(googleFeedService.generateGoogleProductsFeed());
    }
}
