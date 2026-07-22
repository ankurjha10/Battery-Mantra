package com.api.batterymantra.repository;

import com.api.batterymantra.entity.PartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, UUID> {
    Optional<PartnerProfile> findByUserUserId(UUID userId);
    Optional<PartnerProfile> findFirstByOperatingCities_CityNameIgnoreCase(String cityName);
    Optional<PartnerProfile> findFirstByIsActiveTrueAndOperatingCities_CityId(UUID cityId);
    Optional<PartnerProfile> findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(String cityName);
}
