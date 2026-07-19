package com.api.batterymantra.service;

import com.api.batterymantra.dto.vehicle.CreateVehicleRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
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
        res.setMake(v.getMake());
        res.setModel(v.getModel());
        if (v.getFuel() != null) {
            res.setFuelId(v.getFuel().getFuelId());
            res.setFuelName(v.getFuel().getFuelName());
        }
        res.setVehicleType(v.getVehicleType());
        res.setImageUrl(v.getImageUrl());
        res.setCapacity(v.getCapacity());
        res.setSeo(v.getSeo());
        res.setDescription(v.getDescription());
        res.setShortDescription(v.getShortDescription());
        res.setShortDescriptionDealer(v.getShortDescriptionDealer());
        if (v.getCategory() != null) res.setCategoryId(v.getCategory().getCategoryId());
        if (v.getManufacturer() != null) res.setManufacturerId(v.getManufacturer().getId());
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
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setImageUrl(dto.getImageUrl());
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setDescription(dto.getDescription());
        vehicle.setShortDescription(dto.getShortDescription());
        vehicle.setShortDescriptionDealer(dto.getShortDescriptionDealer());
        
        if (dto.getSeo() != null) {
            vehicle.setSeo(dto.getSeo());
        }

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(vehicle::setCategory);
        }
        if (dto.getManufacturerId() != null) {
            manufacturerRepository.findById(dto.getManufacturerId()).ifPresent(vehicle::setManufacturer);
        }
        if (dto.getFuelId() != null) {
            fuelRepository.findById(dto.getFuelId()).ifPresent(vehicle::setFuel);
        }

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

        if (dto.getVehicleType() != null)
            vehicle.setVehicleType(dto.getVehicleType());

        if (dto.getImageUrl() != null)
            vehicle.setImageUrl(dto.getImageUrl());

        if (dto.getCapacity() != null)
            vehicle.setCapacity(dto.getCapacity());

        if (dto.getDescription() != null) vehicle.setDescription(dto.getDescription());
        if (dto.getShortDescription() != null) vehicle.setShortDescription(dto.getShortDescription());
        if (dto.getShortDescriptionDealer() != null) vehicle.setShortDescriptionDealer(dto.getShortDescriptionDealer());
        if (dto.getSeo() != null) vehicle.setSeo(dto.getSeo());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(vehicle::setCategory);
        }
        if (dto.getManufacturerId() != null) {
            manufacturerRepository.findById(dto.getManufacturerId()).ifPresent(vehicle::setManufacturer);
        }
        if (dto.getFuelId() != null) {
            fuelRepository.findById(dto.getFuelId()).ifPresent(vehicle::setFuel);
        }

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
