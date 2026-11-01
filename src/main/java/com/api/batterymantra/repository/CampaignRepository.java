package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID>, JpaSpecificationExecutor<Campaign> {

    Optional<Campaign> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Campaign> findByCouponCodeIgnoreCase(String couponCode);

    @Query("SELECT c FROM Campaign c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now ORDER BY c.priority DESC, c.discountValue DESC, c.createdAt DESC")
    List<Campaign> findActiveCampaigns(@Param("now") OffsetDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now AND (c.couponCode IS NULL OR TRIM(c.couponCode) = '') ORDER BY c.priority DESC, c.discountValue DESC, c.createdAt DESC")
    List<Campaign> findActiveAutoApplyCampaigns(@Param("now") OffsetDateTime now);
}
