package com.api.batterymantra.repository;

import com.api.batterymantra.entity.SpecUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpecUnitRepository extends JpaRepository<SpecUnit, UUID> {
    List<SpecUnit> findBySpecAttribute_IdOrderByValueAsc(UUID specAttributeId);
    List<SpecUnit> findByCategory_CategoryIdOrderByValueAsc(UUID categoryId);
}
