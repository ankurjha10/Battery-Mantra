package com.api.batterymantra.config;

import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Manufacturer;
import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.ManufacturerRepository;
import com.api.batterymantra.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * One-time migration: auto-links existing Manufacturers to Categories
 * based on existing Vehicle data (Vehicle.make -> Manufacturer.name,
 * Vehicle.category -> Category).
 *
 * Runs on startup. Skips manufacturers that already have categories assigned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManufacturerCategoryMigration implements ApplicationRunner {

    private final ManufacturerRepository manufacturerRepository;
    private final VehicleRepository vehicleRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Manufacturer> allManufacturers = manufacturerRepository.findAll();

        // Only process manufacturers that have no categories yet
        List<Manufacturer> unlinked = allManufacturers.stream()
                .filter(m -> m.getCategories() == null || m.getCategories().isEmpty())
                .collect(Collectors.toList());

        if (unlinked.isEmpty()) {
            log.info("ManufacturerCategoryMigration: All manufacturers already have categories. Skipping.");
            return;
        }

        log.info("ManufacturerCategoryMigration: Found {} manufacturers without categories. Migrating...", unlinked.size());

        // Build a map: lowercase manufacturer name -> Manufacturer entity
        Map<String, Manufacturer> nameToManufacturer = unlinked.stream()
                .collect(Collectors.toMap(
                        m -> m.getName().toLowerCase(),
                        m -> m,
                        (a, b) -> a // in case of duplicates, keep first
                ));

        // Get all vehicles
        List<Vehicle> allVehicles = vehicleRepository.findAll();

        // Group: lowercase make -> Set of Category IDs
        Map<String, Set<UUID>> makeToCategoryIds = new HashMap<>();
        for (Vehicle v : allVehicles) {
            if (v.getMake() != null && v.getCategory() != null) {
                String lowerMake = v.getMake().toLowerCase();
                makeToCategoryIds
                        .computeIfAbsent(lowerMake, k -> new HashSet<>())
                        .add(v.getCategory().getCategoryId());
            }
        }

        int updated = 0;
        for (Map.Entry<String, Set<UUID>> entry : makeToCategoryIds.entrySet()) {
            Manufacturer m = nameToManufacturer.get(entry.getKey());
            if (m != null && !entry.getValue().isEmpty()) {
                List<Category> categories = categoryRepository.findAllById(entry.getValue());
                if (!categories.isEmpty()) {
                    m.setCategories(new ArrayList<>(categories));
                    manufacturerRepository.save(m);
                    updated++;
                    log.info("ManufacturerCategoryMigration: Linked '{}' to categories: {}",
                            m.getName(),
                            categories.stream().map(Category::getCategoryName).collect(Collectors.joining(", ")));
                }
            }
        }

        log.info("ManufacturerCategoryMigration: Migration complete. Updated {} manufacturers.", updated);
    }
}
