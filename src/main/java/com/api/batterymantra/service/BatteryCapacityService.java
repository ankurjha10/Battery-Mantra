package com.api.batterymantra.service;

import com.api.batterymantra.dto.capacity.CapacityRequest;
import com.api.batterymantra.dto.capacity.CapacityResponse;
import com.api.batterymantra.entity.BatteryCapacity;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.BatteryCapacityRepository;
import com.api.batterymantra.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatteryCapacityService {

    private final BatteryCapacityRepository capacityRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CapacityResponse> getAllCapacities(UUID categoryId) {
        List<BatteryCapacity> capacities;
        if (categoryId != null) {
            capacities = capacityRepository.findByCategory_CategoryId(categoryId);
        } else {
            capacities = capacityRepository.findAll();
        }
        return capacities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public CapacityResponse createCapacity(CapacityRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        BatteryCapacity capacity = new BatteryCapacity();
        capacity.setCategory(category);
        capacity.setCapacityName(request.getCapacityName());

        BatteryCapacity saved = capacityRepository.save(capacity);
        return mapToResponse(saved);
    }

    @Transactional
    public CapacityResponse updateCapacity(UUID id, CapacityRequest request) {
        BatteryCapacity capacity = capacityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capacity not found"));

        if (!capacity.getCategory().getCategoryId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            capacity.setCategory(category);
        }

        capacity.setCapacityName(request.getCapacityName());

        BatteryCapacity saved = capacityRepository.save(capacity);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCapacity(UUID id) {
        if (!capacityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Capacity not found");
        }
        capacityRepository.deleteById(id);
    }

    private CapacityResponse mapToResponse(BatteryCapacity capacity) {
        return new CapacityResponse(
                capacity.getCapacityId(),
                capacity.getCategory().getCategoryId(),
                capacity.getCapacityName()
        );
    }
}
