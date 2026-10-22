package com.api.batterymantra.service;

import com.api.batterymantra.dto.vehicle.CreateVehicleRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
import com.api.batterymantra.dto.vehicle.UpdateVehicleRequest;
import com.api.batterymantra.entity.Fuel;
import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.FuelRepository;
import com.api.batterymantra.repository.ManufacturerRepository;
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
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final FuelRepository fuelRepository;

    private VehicleResponse toResponse(Vehicle v) {
        VehicleResponse res = new VehicleResponse();
        res.setVehicleId(v.getVehicleId());
        res.setModel(v.getModel());
        res.setSlug(v.getSlug());
        if (v.getFuel() != null) {
            res.setFuelId(v.getFuel().getFuelId());
            res.setFuelName(v.getFuel().getFuelName());
        }
        res.setVehicleType(v.getVehicleType());
        res.setImageUrl(v.getImageUrl());
        res.setCapacity(v.getCapacity());
        if (v.getCategory() != null) res.setCategoryId(v.getCategory().getCategoryId());
        if (v.getManufacturer() != null) {
            res.setManufacturerId(v.getManufacturer().getId());
            res.setMake(v.getManufacturer().getName());
        }
        return res;
    }

    @Cacheable(value = "vehicles")
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "manufacturer.name").and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "model"))).stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "vehicles", key = "#vehicleId")
    public VehicleResponse getVehicleById(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));
    }

    @Cacheable(value = "vehicles", key = "'slug-' + #slug")
    public VehicleResponse getVehicleBySlug(String slug) {
        return vehicleRepository.findBySlug(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with slug: " + slug));
    }

    @Cacheable(value = "vehicles", key = "#make + '-' + #model")
    public List<VehicleResponse> searchVehicles(String make, String model) {
        if (make != null && model != null) {
            return vehicleRepository.findByManufacturerNameIgnoreCaseAndModelIgnoreCase(make, model)
                    .stream().map(this::toResponse).toList();
        } else if (make != null) {
            return vehicleRepository.findByManufacturerNameIgnoreCase(make)
                    .stream().map(this::toResponse).toList();
        }
        return getAllVehicles();
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse createVehicle(CreateVehicleRequest dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setModel(dto.getModel());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setImageUrl(dto.getImageUrl());
        vehicle.setCapacity(dto.getCapacity());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(vehicle::setCategory);
        }
        if (dto.getManufacturerId() != null) {
            manufacturerRepository.findById(dto.getManufacturerId()).ifPresent(vehicle::setManufacturer);
        }
        Fuel fuel = null;
        if (dto.getFuelId() != null) {
            fuel = fuelRepository.findById(dto.getFuelId()).orElse(null);
            if (fuel != null) {
                vehicle.setFuel(fuel);
            }
        }

        String slug = dto.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            String fuelName = fuel != null ? fuel.getFuelName() : "";
            String makeName = dto.getMake() != null && !dto.getMake().trim().isEmpty()
                    ? dto.getMake()
                    : (vehicle.getManufacturer() != null ? vehicle.getManufacturer().getName() : "");
            slug = (makeName + "-" + dto.getModel() + (fuelName.isEmpty() ? "" : "-" + fuelName))
                    .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        }
        vehicle.setSlug(slug);

        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest dto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehicle not found with id: " + vehicleId));

        if (dto.getModel() != null)
            vehicle.setModel(dto.getModel());

        if (dto.getVehicleType() != null)
            vehicle.setVehicleType(dto.getVehicleType());

        if (dto.getImageUrl() != null)
            vehicle.setImageUrl(dto.getImageUrl());

        if (dto.getCapacity() != null)
            vehicle.setCapacity(dto.getCapacity());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(vehicle::setCategory);
        }
        if (dto.getManufacturerId() != null) {
            manufacturerRepository.findById(dto.getManufacturerId()).ifPresent(vehicle::setManufacturer);
        }
        if (dto.getFuelId() != null) {
            fuelRepository.findById(dto.getFuelId()).ifPresent(vehicle::setFuel);
        }

        if (dto.getSlug() != null && !dto.getSlug().trim().isEmpty()) {
            vehicle.setSlug(dto.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", ""));
        } else if (vehicle.getSlug() == null || vehicle.getSlug().trim().isEmpty()) {
            String fuelName = vehicle.getFuel() != null ? vehicle.getFuel().getFuelName() : "";
            String makeName = dto.getMake() != null && !dto.getMake().trim().isEmpty()
                    ? dto.getMake()
                    : (vehicle.getManufacturer() != null ? vehicle.getManufacturer().getName() : "");
            String modelName = vehicle.getModel() != null ? vehicle.getModel() : "";
            String slug = (makeName + "-" + modelName + (fuelName.isEmpty() ? "" : "-" + fuelName))
                    .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
            vehicle.setSlug(slug);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse updateVehicle(UUID vehicleId, CreateVehicleRequest dto) {
        UpdateVehicleRequest updateDto = new UpdateVehicleRequest();
        updateDto.setMake(dto.getMake());
        updateDto.setModel(dto.getModel());
        updateDto.setFuelId(dto.getFuelId());
        updateDto.setVehicleType(dto.getVehicleType());
        updateDto.setImageUrl(dto.getImageUrl());
        updateDto.setCapacity(dto.getCapacity());
        updateDto.setCategoryId(dto.getCategoryId());
        updateDto.setManufacturerId(dto.getManufacturerId());
        updateDto.setSlug(dto.getSlug());
        return updateVehicle(vehicleId, updateDto);
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
