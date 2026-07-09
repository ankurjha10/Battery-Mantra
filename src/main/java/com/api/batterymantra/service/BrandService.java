package com.api.batterymantra.service;

import com.api.batterymantra.dto.brand.BrandRequest;
import com.api.batterymantra.dto.brand.BrandResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.repository.BrandRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    private BrandResponse toBrandResponse(Brand brand) {
        return BrandResponse.builder()
                .brandId(brand.getBrandId())
                .brandName(brand.getBrandName())
                .brandLogo(brand.getBrandLogo())
                .featured(brand.isFeatured())
                .build();
    }

    @Cacheable(value = "brands")
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream().map(this::toBrandResponse).toList();
    }

    @Cacheable(value = "brands", key = "#brandId")
    public BrandResponse getBrandById(UUID brandId) {
        return brandRepository.findById(brandId)
                .map(this::toBrandResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found with Id: " + brandId));
    }

    @Cacheable(value = "brands", key = "'featured'")
    public List<BrandResponse> getFeaturedBrands() {
        return brandRepository.findByFeaturedTrue().stream()
                .map(this::toBrandResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse createBrand(@Valid BrandRequest brandRequest) {
        Brand brand = new Brand();
        brand.setBrandName(brandRequest.getBrandName());
        brand.setBrandLogo(brandRequest.getBrandLogo());
        brand.setFeatured(brandRequest.isFeatured());

        Brand saved = brandRepository.save(brand);
        return toBrandResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateBrand(UUID brandId, @Valid BrandRequest brandRequest) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found with Id: " + brandId));

        if (brandRequest.getBrandName() != null)
            brand.setBrandName(brandRequest.getBrandName());

        if (brandRequest.getBrandLogo() != null)
            brand.setBrandLogo(brandRequest.getBrandLogo());

        brand.setFeatured(brandRequest.isFeatured());

        Brand saved = brandRepository.save(brand);
        return toBrandResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public void deleteBrand(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found with Id: " + brandId));
        brandRepository.delete(brand);
    }
}
