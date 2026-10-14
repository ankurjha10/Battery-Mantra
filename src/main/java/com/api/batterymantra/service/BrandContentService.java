package com.api.batterymantra.service;

import com.api.batterymantra.dto.brandcontent.BrandContentRequest;
import com.api.batterymantra.dto.brandcontent.BrandContentResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.BrandContent;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.repository.BrandContentRepository;
import com.api.batterymantra.repository.BrandRepository;
import com.api.batterymantra.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandContentService {

    private final BrandContentRepository brandContentRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    private BrandContentResponse toResponse(BrandContent bc) {
        return BrandContentResponse.builder()
                .id(bc.getId())
                .categoryId(bc.getCategory() != null ? bc.getCategory().getCategoryId() : null)
                .categoryName(bc.getCategory() != null ? bc.getCategory().getCategoryName() : null)
                .brandId(bc.getBrand() != null ? bc.getBrand().getBrandId() : null)
                .brandName(bc.getBrand() != null ? bc.getBrand().getBrandName() : null)
                .normalShortDescription(bc.getNormalShortDescription())
                .normalDescription(bc.getNormalDescription())
                .normalSeoTitle(bc.getNormalSeoTitle())
                .dealerShortDescription(bc.getDealerShortDescription())
                .dealerDescription(bc.getDealerDescription())
                .dealerSeoTitle(bc.getDealerSeoTitle())
                .createdAt(bc.getCreatedAt())
                .updatedAt(bc.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BrandContentResponse getByCategoryIdAndBrandId(UUID categoryId, UUID brandId) {
        return brandContentRepository.findByCategoryIdAndBrandId(categoryId, brandId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<BrandContentResponse> getAll() {
        return brandContentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BrandContentResponse saveOrUpdate(BrandContentRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + request.getCategoryId()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found with id: " + request.getBrandId()));

        BrandContent content = brandContentRepository.findByCategoryIdAndBrandId(category.getCategoryId(), brand.getBrandId())
                .orElseGet(() -> BrandContent.builder().category(category).brand(brand).build());

        content.setNormalShortDescription(request.getNormalShortDescription());
        content.setNormalDescription(request.getNormalDescription());
        content.setNormalSeoTitle(request.getNormalSeoTitle());
        content.setDealerShortDescription(request.getDealerShortDescription());
        content.setDealerDescription(request.getDealerDescription());
        content.setDealerSeoTitle(request.getDealerSeoTitle());

        BrandContent saved = brandContentRepository.save(content);
        return toResponse(saved);
    }
}
