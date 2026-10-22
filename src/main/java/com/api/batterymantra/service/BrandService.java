package com.api.batterymantra.service;

import com.api.batterymantra.dto.brand.BrandRequest;
import com.api.batterymantra.dto.brand.BrandResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.SeoMetadata;
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
                .description(brand.getDescription())
                .dealerDescription(brand.getDealerDescription())
                .productCount(brand.getProductCount() != null ? brand.getProductCount() : 0)
                .slug(brand.getSlug())
                .build();
    }

    @Cacheable(value = "brands", key = "#categoryId != null ? #categoryId : 'all'")
    public List<BrandResponse> getAllBrands(UUID categoryId) {
        List<Brand> brands;
        if (categoryId != null) {
            brands = brandRepository.findBrandsByCategoryId(categoryId);
        } else {
            brands = brandRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "brandName"));
        }
        return brands.stream().map(this::toBrandResponse).toList();
    }

    @Cacheable(value = "brands", key = "#brandId")
    public BrandResponse getBrandById(UUID brandId) {
        return brandRepository.findById(brandId)
                .map(this::toBrandResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found with Id: " + brandId));
    }

    @Cacheable(value = "brands", key = "'slug-' + #slug")
    public BrandResponse getBrandBySlug(String slug) {
        return brandRepository.findBySlug(slug)
                .or(() -> brandRepository.findBySeo_Slug(slug))
                .map(this::toBrandResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found with slug: " + slug));
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
        brand.setDescription(brandRequest.getDescription());
        brand.setDealerDescription(brandRequest.getDealerDescription());

        String slug = brandRequest.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = brandRequest.getBrandName() != null
                    ? brandRequest.getBrandName().toLowerCase().replace(" ", "-")
                    : "";
        }
        brand.setSlug(slug);
        if (brand.getSeo() == null) {
            brand.setSeo(new SeoMetadata());
        }
        brand.getSeo().setSlug(slug);

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
        
        if (brandRequest.getDescription() != null)
            brand.setDescription(brandRequest.getDescription());

        if (brandRequest.getDealerDescription() != null) {
            brand.setDealerDescription(brandRequest.getDealerDescription());
        }

        String slug = brandRequest.getSlug();
        if (slug != null && !slug.trim().isEmpty()) {
            brand.setSlug(slug.trim());
            if (brand.getSeo() == null) {
                brand.setSeo(new SeoMetadata());
            }
            brand.getSeo().setSlug(slug.trim());
        } else if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
            String fallback = brand.getBrandName() != null
                    ? brand.getBrandName().toLowerCase().replace(" ", "-")
                    : "";
            brand.setSlug(fallback);
            if (brand.getSeo() == null) {
                brand.setSeo(new SeoMetadata());
            }
            if (brand.getSeo().getSlug() == null || brand.getSeo().getSlug().trim().isEmpty()) {
                brand.getSeo().setSlug(fallback);
            }
        }

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
