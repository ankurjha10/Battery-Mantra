package com.api.batterymantra.service;

import com.api.batterymantra.dto.reel.CreateReelRequest;
import com.api.batterymantra.dto.reel.ReelResponse;
import com.api.batterymantra.dto.reel.UpdateReelRequest;
import com.api.batterymantra.entity.InstagramReel;
import com.api.batterymantra.repository.InstagramReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstagramReelService {

    private final InstagramReelRepository reelRepository;

    public List<ReelResponse> getAllReels() {
        return reelRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReelResponse> getActiveReels() {
        return reelRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReelResponse createReel(CreateReelRequest request) {
        InstagramReel reel = new InstagramReel();
        reel.setUrl(request.getUrl());
        reel.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        reel.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        return mapToResponse(reelRepository.save(reel));
    }

    @Transactional
    public ReelResponse updateReel(UUID id, UpdateReelRequest request) {
        InstagramReel reel = reelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reel not found"));
        reel.setUrl(request.getUrl());
        if (request.getIsActive() != null) reel.setIsActive(request.getIsActive());
        if (request.getDisplayOrder() != null) reel.setDisplayOrder(request.getDisplayOrder());
        return mapToResponse(reelRepository.save(reel));
    }

    @Transactional
    public void deleteReel(UUID id) {
        reelRepository.deleteById(id);
    }

    private ReelResponse mapToResponse(InstagramReel reel) {
        ReelResponse response = new ReelResponse();
        response.setReelId(reel.getReelId());
        response.setUrl(reel.getUrl());
        response.setIsActive(reel.getIsActive());
        response.setDisplayOrder(reel.getDisplayOrder());
        return response;
    }
}
