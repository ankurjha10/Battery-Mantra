package com.api.batterymantra.controller;

import com.api.batterymantra.dto.reel.ReelResponse;
import com.api.batterymantra.service.InstagramReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class InstagramReelController {

    private final InstagramReelService reelService;

    @GetMapping("/active")
    public ResponseEntity<List<ReelResponse>> getActiveReels() {
        return ResponseEntity.ok(reelService.getActiveReels());
    }
}
