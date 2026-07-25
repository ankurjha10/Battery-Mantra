package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
    List<Manufacturer> findAllByOrderByDisplayOrderAsc();

    boolean existsByName(String name);

    List<Manufacturer> findDistinctByCategoriesCategoryIdOrderByDisplayOrderAsc(UUID categoryId);
}
