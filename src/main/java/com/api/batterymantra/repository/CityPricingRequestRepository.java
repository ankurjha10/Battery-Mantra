package com.api.batterymantra.repository;

import com.api.batterymantra.entity.CityPricingRequest;
import com.api.batterymantra.entity.enums.PricingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CityPricingRequestRepository extends JpaRepository<CityPricingRequest, UUID> {

    List<CityPricingRequest> findByPartner_IdOrderByCreatedAtDesc(UUID partnerId);

    List<CityPricingRequest> findByPartner_IdAndStatusOrderByCreatedAtDesc(UUID partnerId, PricingRequestStatus status);

    List<CityPricingRequest> findByStatusOrderByCreatedAtDesc(PricingRequestStatus status);

    List<CityPricingRequest> findAllByOrderByCreatedAtDesc();
}

