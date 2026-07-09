package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.entity.enums.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByMakeIgnoreCase(String make);
    List<Vehicle> findByMakeIgnoreCaseAndModelIgnoreCase(String make, String model);
    List<Vehicle> findByFuelType(FuelType fuelType);
}
