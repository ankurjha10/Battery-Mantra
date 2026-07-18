package com.api.batterymantra.repository;

import com.api.batterymantra.entity.BatteryCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatteryCapacityRepository extends JpaRepository<BatteryCapacity, UUID> {
    List<BatteryCapacity> findByCategory_CategoryId(UUID categoryId);
}
