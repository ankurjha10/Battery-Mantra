package com.api.batterymantra.service;

import com.api.batterymantra.dto.manufacturer.CreateManufacturerRequest;
import com.api.batterymantra.dto.manufacturer.ManufacturerResponse;
import com.api.batterymantra.dto.manufacturer.UpdateManufacturerRequest;
import com.api.batterymantra.entity.Manufacturer;
import com.api.batterymantra.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    public List<ManufacturerResponse> getAllManufacturers() {
        return manufacturerRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ManufacturerResponse getManufacturerById(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found with id: " + id));
        return mapToResponse(manufacturer);
    }

    public ManufacturerResponse createManufacturer(CreateManufacturerRequest request) {
        if (manufacturerRepository.existsByName(request.getName())) {
            throw new RuntimeException("Manufacturer with name '" + request.getName() + "' already exists");
        }

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(request.getName());
        manufacturer.setLogoUrl(request.getLogoUrl());
        manufacturer.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);
        return mapToResponse(savedManufacturer);
    }

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
        return response;
    }
}
