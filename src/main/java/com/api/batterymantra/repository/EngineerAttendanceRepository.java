package com.api.batterymantra.repository;

import com.api.batterymantra.entity.EngineerAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EngineerAttendanceRepository extends JpaRepository<EngineerAttendance, UUID> {
    Optional<EngineerAttendance> findByEngineerIdAndDate(UUID engineerId, LocalDate date);
    List<EngineerAttendance> findByEngineerIdOrderByDateDesc(UUID engineerId);
}
