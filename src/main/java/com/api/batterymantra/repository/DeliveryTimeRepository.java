package com.api.batterymantra.repository;

import com.api.batterymantra.entity.DeliveryTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryTimeRepository extends JpaRepository<DeliveryTime, UUID> {
    Optional<DeliveryTime> findByCategory_CategoryIdAndCity_CityId(UUID categoryId, UUID cityId);
}
