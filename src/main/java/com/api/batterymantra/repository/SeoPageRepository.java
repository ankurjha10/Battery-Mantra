package com.api.batterymantra.repository;

import com.api.batterymantra.entity.SeoPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeoPageRepository extends JpaRepository<SeoPage, UUID> {
    Optional<SeoPage> findByPageRoute(String pageRoute);
}
