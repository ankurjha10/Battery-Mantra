package com.api.batterymantra.repository;

import com.api.batterymantra.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEngineerIdOrderByAppliedAtDesc(UUID engineerId);
}
