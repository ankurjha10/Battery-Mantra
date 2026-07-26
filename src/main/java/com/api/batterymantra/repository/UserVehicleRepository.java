package com.api.batterymantra.repository;

import com.api.batterymantra.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle, UUID> {
    List<UserVehicle> findByUserUserId(UUID userId);
}
