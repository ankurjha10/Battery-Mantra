package com.api.batterymantra.service;

import com.api.batterymantra.dto.vehicle.CreateVehicleRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.repository.VehicleRepository;
import jakarta.transaction.Transactional;
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
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    private VehicleResponse toResponse(Vehicle v) {
        VehicleResponse res = new VehicleResponse();
        res.setVehicleId(v.getVehicleId());
        res.setMake(v.getMake());
        res.setModel(v.getModel());
        res.setYearFrom(v.getYearFrom());
        res.setYearTo(v.getYearTo());
        res.setFuelType(v.getFuelType());
        res.setImageUrl(v.getImageUrl());
        return res;
    }

    @Cacheable(value = "vehicles")
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "vehicles", key = "#vehicleId")
    public VehicleResponse getVehicleById(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));
    }

    @Cacheable(value = "vehicles", key = "#make + '-' + #model")
    public List<VehicleResponse> searchVehicles(String make, String model) {
        if (make != null && model != null) {
            return vehicleRepository.findByMakeIgnoreCaseAndModelIgnoreCase(make, model)
                    .stream().map(this::toResponse).toList();
        } else if (make != null) {
            return vehicleRepository.findByMakeIgnoreCase(make)
                    .stream().map(this::toResponse).toList();
        }
        return getAllVehicles();
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse createVehicle(CreateVehicleRequest dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setMake(dto.getMake());
        vehicle.setModel(dto.getModel());
        vehicle.setYearFrom(dto.getYearFrom());
        vehicle.setYearTo(dto.getYearTo());
        vehicle.setFuelType(dto.getFuelType());
        vehicle.setImageUrl(dto.getImageUrl());

        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse updateVehicle(UUID vehicleId, CreateVehicleRequest dto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));

        if (dto.getMake() != null)
            vehicle.setMake(dto.getMake());

        if (dto.getModel() != null)
            vehicle.setModel(dto.getModel());

        if (dto.getYearFrom() != null)
            vehicle.setYearFrom(dto.getYearFrom());

        if (dto.getYearTo() != null)
            vehicle.setYearTo(dto.getYearTo());

        if (dto.getFuelType() != null)
            vehicle.setFuelType(dto.getFuelType());

        if (dto.getImageUrl() != null)
            vehicle.setImageUrl(dto.getImageUrl());

        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public void deleteVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));
        vehicleRepository.delete(vehicle);
    }
}
