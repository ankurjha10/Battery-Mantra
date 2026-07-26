package com.api.batterymantra.repository;

import com.api.batterymantra.entity.EnquiryRequest;
import com.api.batterymantra.entity.enums.EnquiryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnquiryRequestRepository extends JpaRepository<EnquiryRequest, Long> {
    List<EnquiryRequest> findAllByOrderByCreatedAtDesc();
    List<EnquiryRequest> findAllByEnquiryTypeOrderByCreatedAtDesc(EnquiryType enquiryType);
}
