package com.api.batterymantra.repository;

import com.api.batterymantra.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    @Override
    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.engineer e JOIN FETCH e.user ORDER BY l.appliedAt DESC")
    List<LeaveRequest> findAll();

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.engineer e JOIN FETCH e.user WHERE e.id = :engineerId ORDER BY l.appliedAt DESC")
    List<LeaveRequest> findByEngineerIdOrderByAppliedAtDesc(@Param("engineerId") UUID engineerId);
}
