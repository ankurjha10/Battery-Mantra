package com.api.batterymantra.service;

import com.api.batterymantra.dto.banner.BannerResponse;
import com.api.batterymantra.dto.banner.CreateBannerRequest;
import com.api.batterymantra.dto.banner.UpdateBannerRequest;
import com.api.batterymantra.entity.Banner;
import com.api.batterymantra.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    private BannerResponse toResponse(Banner banner) {
        BannerResponse response = new BannerResponse();
        response.setBannerId(banner.getBannerId());
        response.setTitle(banner.getTitle());
        response.setImageUrl(banner.getImageUrl());
        response.setLinkUrl(banner.getLinkUrl());
        response.setIsActive(banner.getIsActive());
        response.setDisplayOrder(banner.getDisplayOrder());
        return response;
    }

    public List<BannerResponse> getAllActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerResponse createBanner(CreateBannerRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        if (request.getIsActive() != null) {
            banner.setIsActive(request.getIsActive());
        }
        banner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        
        Banner saved = bannerRepository.save(banner);
        return toResponse(saved);
    }

    @Transactional
    public BannerResponse updateBanner(UUID id, UpdateBannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Banner not found"));

        if (request.getTitle() != null) banner.setTitle(request.getTitle());
        if (request.getImageUrl() != null) banner.setImageUrl(request.getImageUrl());
        if (request.getLinkUrl() != null) banner.setLinkUrl(request.getLinkUrl());
        if (request.getIsActive() != null) banner.setIsActive(request.getIsActive());
        if (request.getDisplayOrder() != null) banner.setDisplayOrder(request.getDisplayOrder());

        Banner updated = bannerRepository.save(banner);
        return toResponse(updated);
    }

    @Transactional
    public void deleteBanner(UUID id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Banner not found");
        }
        bannerRepository.deleteById(id);
    }
}
