package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Fuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface FuelRepository extends JpaRepository<Fuel, UUID> {
    Optional<Fuel> findByFuelNameIgnoreCase(String fuelName);
}
