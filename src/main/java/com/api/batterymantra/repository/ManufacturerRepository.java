package com.api.batterymantra.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.batterymantra.entity.Manufacturer;
import com.api.batterymantra.entity.enums.VehicleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
    List<Manufacturer> findAllByOrderByDisplayOrderAsc();

    boolean existsByName(String name);

    @Query("SELECT DISTINCT m FROM Manufacturer m WHERE m.name IN (SELECT v.make FROM Vehicle v WHERE v.vehicleType = :type) ORDER BY m.displayOrder ASC")
    List<Manufacturer> findDistinctByVehicleType(@Param("type") VehicleType type);
}
