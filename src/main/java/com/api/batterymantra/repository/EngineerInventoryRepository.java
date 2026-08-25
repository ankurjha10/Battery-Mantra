package com.api.batterymantra.repository;

import com.api.batterymantra.entity.EngineerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface EngineerInventoryRepository extends JpaRepository<EngineerInventory, UUID> {
    Optional<EngineerInventory> findByEngineerIdAndProductProductId(UUID engineerId, UUID productId);
    List<EngineerInventory> findByEngineerId(UUID engineerId);
}
