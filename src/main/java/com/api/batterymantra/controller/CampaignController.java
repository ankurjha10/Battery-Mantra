package com.api.batterymantra.controller;

import com.api.batterymantra.dto.campaign.CampaignLandingResponse;
import com.api.batterymantra.dto.campaign.CampaignResponse;
import com.api.batterymantra.dto.campaign.ValidateCartRequest;
import com.api.batterymantra.dto.campaign.ValidateCartResponse;
import com.api.batterymantra.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    /**
     * Returns all currently active campaigns for Announcement bar and Home page spotlights.
     */
    @GetMapping("/active")
    public ResponseEntity<List<CampaignResponse>> getActiveCampaigns() {
        return ResponseEntity.ok(campaignService.getActiveCampaigns());
    }

    /**
     * Returns single campaign details with paginated list of participating products
     * for the dedicated landing page /offers/:slug.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<CampaignLandingResponse> getCampaignBySlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) UUID cityId) {

        return ResponseEntity.ok(campaignService.getCampaignBySlug(slug, page, size, category, brand, sort, cityId));
    }

    /**
     * Calculates applicable campaign discounts for the cart items before checkout.
     */
    @PostMapping("/validate-cart")
    public ResponseEntity<ValidateCartResponse> validateCart(@RequestBody @Valid ValidateCartRequest request) {
        return ResponseEntity.ok(campaignService.validateCart(request));
    }
}
