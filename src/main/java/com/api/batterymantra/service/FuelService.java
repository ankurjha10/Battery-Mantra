package com.api.batterymantra.service;

import com.api.batterymantra.entity.Fuel;
import com.api.batterymantra.repository.FuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FuelService {

    private final FuelRepository fuelRepository;

    public List<Fuel> getAllFuels() {
        return fuelRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
    }

    @Transactional
    public Fuel createFuel(Fuel fuel) {
        if (fuelRepository.findByFuelNameIgnoreCase(fuel.getFuelName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuel with this name already exists");
        }
        return fuelRepository.save(fuel);
    }

    @Transactional
    public Fuel updateFuel(UUID fuelId, Fuel fuelData) {
        Fuel fuel = fuelRepository.findById(fuelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuel not found"));
        
        fuel.setFuelName(fuelData.getFuelName());
        if (fuelData.getDisplayOrder() != null) {
            fuel.setDisplayOrder(fuelData.getDisplayOrder());
        }
        return fuelRepository.save(fuel);
    }

    @Transactional
    public void deleteFuel(UUID fuelId) {
        fuelRepository.deleteById(fuelId);
    }
}
