package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
    List<Manufacturer> findAllByOrderByDisplayOrderAsc();
    boolean existsByName(String name);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT v.manufacturer FROM Vehicle v WHERE v.vehicleType = :type ORDER BY v.manufacturer.displayOrder ASC")
    List<Manufacturer> findDistinctByVehicleType(@org.springframework.data.repository.query.Param("type") com.api.batterymantra.entity.enums.VehicleType type);
}
