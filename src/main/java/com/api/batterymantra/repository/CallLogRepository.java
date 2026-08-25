package com.api.batterymantra.repository;

import com.api.batterymantra.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CallLogRepository extends JpaRepository<CallLog, UUID> {
    List<CallLog> findByOrderOrderId(UUID orderId);
    List<CallLog> findByEngineerId(UUID engineerId);
}
