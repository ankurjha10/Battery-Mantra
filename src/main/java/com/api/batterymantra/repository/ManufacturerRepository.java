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
}
