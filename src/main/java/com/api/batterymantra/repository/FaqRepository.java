package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Faq;
import com.api.batterymantra.enums.FaqPageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {
    List<Faq> findByPageTypeAndIsActiveTrue(FaqPageType pageType);
}
