package com.api.batterymantra.repository;

import com.api.batterymantra.entity.SpecCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpecCategoryRepository extends JpaRepository<SpecCategory, UUID> {
    List<SpecCategory> findByCategory_CategoryIdOrderByNameAsc(UUID categoryId);
}
