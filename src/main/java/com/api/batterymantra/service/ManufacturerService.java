package com.api.batterymantra.service;

import com.api.batterymantra.dto.manufacturer.CreateManufacturerRequest;
import com.api.batterymantra.dto.manufacturer.ManufacturerResponse;
import com.api.batterymantra.dto.manufacturer.UpdateManufacturerRequest;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Manufacturer;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;
    private final CategoryRepository categoryRepository;

    public List<ManufacturerResponse> getAllManufacturers(UUID categoryId) {
        List<Manufacturer> manufacturers;
        if (categoryId != null) {
            manufacturers = manufacturerRepository.findDistinctByCategoriesCategoryIdOrderByDisplayOrderAsc(categoryId);
        } else {
            manufacturers = manufacturerRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "displayOrder").and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name")));
        }
        return manufacturers.stream()
                .sorted(java.util.Comparator.comparing((Manufacturer m) -> m.getDisplayOrder()).thenComparing(m -> m.getName()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ManufacturerResponse getManufacturerById(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found with id: " + id));
        return mapToResponse(manufacturer);
    }

    @Transactional
    public ManufacturerResponse createManufacturer(CreateManufacturerRequest request) {
        if (manufacturerRepository.existsByName(request.getName())) {
            throw new RuntimeException("Manufacturer with name '" + request.getName() + "' already exists");
        }

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(request.getName());
        manufacturer.setLogoUrl(request.getLogoUrl());
        manufacturer.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        manufacturer.setDescription(request.getDescription());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            manufacturer.setCategories(categories);
        }

        Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);
        return mapToResponse(savedManufacturer);
    }

    @Transactional
    public ManufacturerResponse updateManufacturer(UUID id, UpdateManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(manufacturer.getName())) {
            if (manufacturerRepository.existsByName(request.getName())) {
                throw new RuntimeException("Manufacturer with name '" + request.getName() + "' already exists");
            }
            manufacturer.setName(request.getName());
        }

        if (request.getLogoUrl() != null) {
            manufacturer.setLogoUrl(request.getLogoUrl());
        }

        if (request.getDisplayOrder() != null) {
            manufacturer.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getDescription() != null) {
            manufacturer.setDescription(request.getDescription());
        }

        if (request.getCategoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            manufacturer.setCategories(categories);
        }

        Manufacturer updatedManufacturer = manufacturerRepository.save(manufacturer);
        return mapToResponse(updatedManufacturer);
    }

    public void deleteManufacturer(UUID id) {
        if (!manufacturerRepository.existsById(id)) {
            throw new RuntimeException("Manufacturer not found with id: " + id);
        }
        manufacturerRepository.deleteById(id);
    }

    private ManufacturerResponse mapToResponse(Manufacturer manufacturer) {
        ManufacturerResponse response = new ManufacturerResponse();
        response.setId(manufacturer.getId());
        response.setName(manufacturer.getName());
        response.setLogoUrl(manufacturer.getLogoUrl());
        response.setDisplayOrder(manufacturer.getDisplayOrder());
        response.setDescription(manufacturer.getDescription());

        if (manufacturer.getCategories() != null) {
            List<ManufacturerResponse.CategoryInfo> categoryInfos = manufacturer.getCategories().stream()
                    .map(cat -> {
                        ManufacturerResponse.CategoryInfo info = new ManufacturerResponse.CategoryInfo();
                        info.setCategoryId(cat.getCategoryId());
                        info.setCategoryName(cat.getCategoryName());
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setCategories(categoryInfos);
        } else {
            response.setCategories(Collections.emptyList());
        }

        return response;
    }
}
