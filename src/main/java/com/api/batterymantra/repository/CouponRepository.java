package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeIgnoreCase(String code);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
           "AND (c.displayOnPdp = true) " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.expiryDate IS NULL OR c.expiryDate >= :now) " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findActivePublicCoupons(@Param("now") LocalDateTime now);
}
