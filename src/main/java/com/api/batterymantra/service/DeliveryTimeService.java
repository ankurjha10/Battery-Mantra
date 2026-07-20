package com.api.batterymantra.service;

import com.api.batterymantra.dto.DeliveryTimeRequest;
import com.api.batterymantra.dto.DeliveryTimeResponse;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.DeliveryTime;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.DeliveryTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryTimeService {

    private final DeliveryTimeRepository deliveryTimeRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;

    public List<DeliveryTimeResponse> getAllDeliveryTimes() {
        return deliveryTimeRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public DeliveryTimeResponse getDeliveryTime(UUID categoryId, UUID cityId) {
        Optional<DeliveryTime> deliveryTime = deliveryTimeRepository.findByCategory_CategoryIdAndCity_CityId(categoryId, cityId);
        if (deliveryTime.isPresent()) {
            return mapToResponse(deliveryTime.get());
        }
        
        // Return an empty response instead of 404 to avoid frontend breaking
        DeliveryTimeResponse empty = new DeliveryTimeResponse();
        empty.setCategoryId(categoryId);
        empty.setCityId(cityId);
        empty.setDays("");
        empty.setHours("");
        return empty;
    }

    @Transactional
    public List<DeliveryTimeResponse> updateDeliveryTimes(List<DeliveryTimeRequest> requests) {
        for (DeliveryTimeRequest request : requests) {
            DeliveryTime deliveryTime = deliveryTimeRepository.findByCategory_CategoryIdAndCity_CityId(request.getCategoryId(), request.getCityId())
                    .orElse(new DeliveryTime());

            if (deliveryTime.getId() == null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.getCategoryId()));
                City city = cityRepository.findById(request.getCityId())
                        .orElseThrow(() -> new ResourceNotFoundException("City not found with id " + request.getCityId()));
                deliveryTime.setCategory(category);
                deliveryTime.setCity(city);
            }

            deliveryTime.setDays(request.getDays());
            deliveryTime.setHours(request.getHours());
            
            deliveryTimeRepository.save(deliveryTime);
        }
        
        return getAllDeliveryTimes();
    }

    private DeliveryTimeResponse mapToResponse(DeliveryTime deliveryTime) {
        DeliveryTimeResponse response = new DeliveryTimeResponse();
        response.setId(deliveryTime.getId());
        response.setCategoryId(deliveryTime.getCategory().getCategoryId());
        response.setCityId(deliveryTime.getCity().getCityId());
        response.setDays(deliveryTime.getDays());
        response.setHours(deliveryTime.getHours());
        return response;
    }
}
