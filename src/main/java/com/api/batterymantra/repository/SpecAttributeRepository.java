package com.api.batterymantra.repository;

import com.api.batterymantra.entity.SpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpecAttributeRepository extends JpaRepository<SpecAttribute, UUID> {
    List<SpecAttribute> findBySpecCategory_IdOrderByNameAsc(UUID specCategoryId);
    List<SpecAttribute> findByCategory_CategoryIdOrderByNameAsc(UUID categoryId);
}
