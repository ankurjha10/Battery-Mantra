package com.api.batterymantra.repository;

import com.api.batterymantra.entity.SeoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeoTemplateRepository extends JpaRepository<SeoTemplate, UUID> {
    Optional<SeoTemplate> findByTemplateType(String templateType);
}
