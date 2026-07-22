package com.api.batterymantra.repository;

import com.api.batterymantra.entity.EngineerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface EngineerProfileRepository extends JpaRepository<EngineerProfile, UUID> {
    Optional<EngineerProfile> findByUserUserId(UUID userId);
}
