package com.api.batterymantra.repository;

import com.api.batterymantra.entity.AdminGateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminGateConfigRepository extends JpaRepository<AdminGateConfig, Long> {
}

