package com.api.batterymantra.controller.admin;

import com.api.batterymantra.dto.campaign.CampaignRequest;
import com.api.batterymantra.dto.campaign.CampaignResponse;
import com.api.batterymantra.entity.enums.CampaignStatusFilter;
import com.api.batterymantra.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN')")
public class AdminCampaignController {

    private final CampaignService campaignService;

    /**
     * Paginated list of all campaigns with status filters (ALL, ACTIVE, SCHEDULED, EXPIRED)
     */
    @GetMapping
    public ResponseEntity<Page<CampaignResponse>> getAllCampaigns(
            @RequestParam(defaultValue = "ALL") CampaignStatusFilter status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(campaignService.getAllCampaigns(status, search, pageable));
    }

    /**
     * Create new campaign with target IDs (categoryIds, brandIds, productIds)
     */
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@RequestBody @Valid CampaignRequest request) {
        return new ResponseEntity<>(campaignService.createCampaign(request), HttpStatus.CREATED);
    }

    /**
     * Fetch campaign details including associated category/brand/product IDs for editing
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    /**
     * Update existing campaign details and associations
     */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest request) {
        return ResponseEntity.ok(campaignService.updateCampaign(id, request));
    }

    /**
     * Remove campaign
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable UUID id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Quick toggle is_active (true/false)
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<CampaignResponse> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.toggleCampaignStatus(id));
    }
}
