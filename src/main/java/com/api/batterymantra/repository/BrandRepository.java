package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    List<Brand> findByFeaturedTrue();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b FROM Brand b JOIN Product p ON p.brand = b WHERE p.productCategory.categoryId = :categoryId AND p.isApproved = true ORDER BY b.brandName ASC")
    List<Brand> findBrandsByCategoryId(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId);
}
