package com.api.batterymantra.service;

import com.api.batterymantra.dto.spec.*;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecService {

    private final SpecCategoryRepository specCategoryRepository;
    private final SpecAttributeRepository specAttributeRepository;
    private final SpecUnitRepository specUnitRepository;
    private final CategoryRepository categoryRepository;

    // ======================== Spec Categories ========================

    public List<SpecCategoryResponse> getSpecCategoriesByCategoryId(UUID categoryId) {
        return specCategoryRepository.findByCategory_CategoryIdOrderByNameAsc(categoryId)
                .stream()
                .map(this::mapToSpecCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecCategoryResponse createSpecCategory(SpecCategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        SpecCategory specCategory = SpecCategory.builder()
                .name(request.getName())
                .category(category)
                .build();

        SpecCategory saved = specCategoryRepository.save(specCategory);
        return mapToSpecCategoryResponse(saved);
    }

    @Transactional
    public void deleteSpecCategory(UUID id) {
        if (!specCategoryRepository.existsById(id)) {
            throw new RuntimeException("Spec Category not found with id: " + id);
        }
        specCategoryRepository.deleteById(id);
    }

    private SpecCategoryResponse mapToSpecCategoryResponse(SpecCategory sc) {
        return SpecCategoryResponse.builder()
                .id(sc.getId())
                .name(sc.getName())
                .categoryId(sc.getCategory().getCategoryId())
                .categoryName(sc.getCategory().getCategoryName())
                .build();
    }

    // ======================== Spec Attributes ========================

    public List<SpecAttributeResponse> getSpecAttributesBySpecCategoryId(UUID specCategoryId) {
        return specAttributeRepository.findBySpecCategory_IdOrderByNameAsc(specCategoryId)
                .stream()
                .map(this::mapToSpecAttributeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecAttributeResponse createSpecAttribute(SpecAttributeRequest request) {
        SpecCategory specCategory = specCategoryRepository.findById(request.getSpecCategoryId())
                .orElseThrow(() -> new RuntimeException("Spec Category not found with id: " + request.getSpecCategoryId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        SpecAttribute specAttribute = SpecAttribute.builder()
                .name(request.getName())
                .specCategory(specCategory)
                .category(category)
                .build();

        SpecAttribute saved = specAttributeRepository.save(specAttribute);
        return mapToSpecAttributeResponse(saved);
    }

    @Transactional
    public void deleteSpecAttribute(UUID id) {
        if (!specAttributeRepository.existsById(id)) {
            throw new RuntimeException("Spec Attribute not found with id: " + id);
        }
        specAttributeRepository.deleteById(id);
    }

    private SpecAttributeResponse mapToSpecAttributeResponse(SpecAttribute sa) {
        return SpecAttributeResponse.builder()
                .id(sa.getId())
                .name(sa.getName())
                .specCategoryId(sa.getSpecCategory().getId())
                .specCategoryName(sa.getSpecCategory().getName())
                .categoryId(sa.getCategory().getCategoryId())
                .categoryName(sa.getCategory().getCategoryName())
                .build();
    }

    // ======================== Spec Units ========================

    public List<SpecUnitResponse> getSpecUnitsByAttributeId(UUID attributeId) {
        return specUnitRepository.findBySpecAttribute_IdOrderByValueAsc(attributeId)
                .stream()
                .map(this::mapToSpecUnitResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecUnitResponse createSpecUnit(SpecUnitRequest request) {
        SpecAttribute specAttribute = specAttributeRepository.findById(request.getSpecAttributeId())
                .orElseThrow(() -> new RuntimeException("Spec Attribute not found with id: " + request.getSpecAttributeId()));

        SpecCategory specCategory = specCategoryRepository.findById(request.getSpecCategoryId())
                .orElseThrow(() -> new RuntimeException("Spec Category not found with id: " + request.getSpecCategoryId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        SpecUnit specUnit = SpecUnit.builder()
                .value(request.getValue())
                .specAttribute(specAttribute)
                .specCategory(specCategory)
                .category(category)
                .build();

        SpecUnit saved = specUnitRepository.save(specUnit);
        return mapToSpecUnitResponse(saved);
    }

    @Transactional
    public void deleteSpecUnit(UUID id) {
        if (!specUnitRepository.existsById(id)) {
            throw new RuntimeException("Spec Unit not found with id: " + id);
        }
        specUnitRepository.deleteById(id);
    }

    private SpecUnitResponse mapToSpecUnitResponse(SpecUnit su) {
        return SpecUnitResponse.builder()
                .id(su.getId())
                .value(su.getValue())
                .specAttributeId(su.getSpecAttribute().getId())
                .specAttributeName(su.getSpecAttribute().getName())
                .specCategoryId(su.getSpecCategory().getId())
                .specCategoryName(su.getSpecCategory().getName())
                .categoryId(su.getCategory().getCategoryId())
                .categoryName(su.getCategory().getCategoryName())
                .build();
    }

    // ======================== Category Spec Template ========================

    @Transactional(readOnly = true)
    public CategorySpecTemplateResponse getCategorySpecTemplate(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        List<SpecCategory> specCategories = specCategoryRepository
                .findByCategory_CategoryIdOrderByNameAsc(categoryId);

        List<CategorySpecTemplateResponse.SpecGroupDto> specGroups = specCategories.stream()
                .map(sc -> {
                    List<SpecAttribute> attributes = specAttributeRepository
                            .findBySpecCategory_IdOrderByNameAsc(sc.getId());

                    List<CategorySpecTemplateResponse.SpecAttributeDto> attributeDtos = attributes.stream()
                            .map(attr -> {
                                List<String> units = specUnitRepository
                                        .findBySpecAttribute_IdOrderByValueAsc(attr.getId())
                                        .stream()
                                        .map(SpecUnit::getValue)
                                        .collect(Collectors.toList());

                                return CategorySpecTemplateResponse.SpecAttributeDto.builder()
                                        .attributeId(attr.getId())
                                        .attributeName(attr.getName())
                                        .availableUnits(units)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return CategorySpecTemplateResponse.SpecGroupDto.builder()
                            .specCategoryId(sc.getId())
                            .specCategoryName(sc.getName())
                            .attributes(attributeDtos)
                            .build();
                })
                .collect(Collectors.toList());

        return CategorySpecTemplateResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .specGroups(specGroups)
                .build();
    }
}
