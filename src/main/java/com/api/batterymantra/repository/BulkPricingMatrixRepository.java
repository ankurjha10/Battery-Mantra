package com.api.batterymantra.repository;

import com.api.batterymantra.entity.BulkPricingMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkPricingMatrixRepository extends JpaRepository<BulkPricingMatrix, UUID> {
    List<BulkPricingMatrix> findByCategory_CategoryIdAndBrand_BrandId(UUID categoryId, UUID brandId);
    Optional<BulkPricingMatrix> findByCategory_CategoryIdAndBrand_BrandIdAndCity_CityId(UUID categoryId, UUID brandId, UUID cityId);
}
