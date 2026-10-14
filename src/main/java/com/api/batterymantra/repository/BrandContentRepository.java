package com.api.batterymantra.repository;

import com.api.batterymantra.entity.BrandContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandContentRepository extends JpaRepository<BrandContent, UUID> {

    @Query("SELECT bc FROM BrandContent bc JOIN FETCH bc.category c JOIN FETCH bc.brand b WHERE c.categoryId = :categoryId AND b.brandId = :brandId")
    Optional<BrandContent> findByCategoryIdAndBrandId(@Param("categoryId") UUID categoryId, @Param("brandId") UUID brandId);

    @Override
    @Query("SELECT bc FROM BrandContent bc JOIN FETCH bc.category c JOIN FETCH bc.brand b")
    List<BrandContent> findAll();
}
