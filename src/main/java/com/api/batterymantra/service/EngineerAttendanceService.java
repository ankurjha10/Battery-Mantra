package com.api.batterymantra.service;

import com.api.batterymantra.dto.engineer.CreateLeaveRequest;
import com.api.batterymantra.entity.EngineerAttendance;
import com.api.batterymantra.entity.EngineerProfile;
import com.api.batterymantra.entity.LeaveRequest;
import com.api.batterymantra.entity.enums.AttendanceStatus;
import com.api.batterymantra.entity.enums.LeaveStatus;
import com.api.batterymantra.repository.EngineerAttendanceRepository;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngineerAttendanceService {

    private final EngineerAttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final EngineerProfileRepository engineerRepository;

    @Transactional
    public EngineerAttendance checkIn(UUID engineerUserId) {
        EngineerProfile engineer = engineerRepository.findByUserUserId(engineerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer profile not found"));

        java.util.Optional<EngineerAttendance> latestOpt = attendanceRepository.findFirstByEngineerIdOrderByDateDesc(engineer.getId());
        if (latestOpt.isPresent()) {
            EngineerAttendance latest = latestOpt.get();
            if (latest.getCheckOutTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please check out of your active shift first.");
            }
            if (LocalDate.now().equals(latest.getDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already completed shift for today");
            }
        }

        EngineerAttendance attendance = EngineerAttendance.builder()
                .engineer(engineer)
                .date(LocalDate.now())
                .checkInTime(LocalDateTime.now())
                .status(AttendanceStatus.PRESENT)
                .build();
        
        engineer.setDutyStatus(com.api.batterymantra.entity.enums.DutyStatus.ON_DUTY);
        engineerRepository.save(engineer);
        
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public EngineerAttendance checkOut(UUID engineerUserId) {
        EngineerProfile engineer = engineerRepository.findByUserUserId(engineerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer profile not found"));

        EngineerAttendance latestAttendance = attendanceRepository.findFirstByEngineerIdOrderByDateDesc(engineer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must check in first"));

        if (latestAttendance.getCheckOutTime() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must check in first");
        }

        latestAttendance.setCheckOutTime(LocalDateTime.now());
        
        engineer.setDutyStatus(com.api.batterymantra.entity.enums.DutyStatus.OFF_DUTY);
        engineerRepository.save(engineer);
        
        return attendanceRepository.save(latestAttendance);
    }

    @Transactional
    public LeaveRequest applyForLeave(UUID engineerUserId, CreateLeaveRequest request) {
        EngineerProfile engineer = engineerRepository.findByUserUserId(engineerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer profile not found"));

        LeaveRequest leave = LeaveRequest.builder()
                .engineer(engineer)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return leaveRepository.save(leave);
    }

    public List<EngineerAttendance> getMyAttendance(UUID engineerUserId) {
        EngineerProfile engineer = engineerRepository.findByUserUserId(engineerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer profile not found"));
        return attendanceRepository.findByEngineerIdOrderByDateDesc(engineer.getId());
    }

    public List<EngineerAttendance> getAttendanceByEngineerId(UUID engineerId) {
        return attendanceRepository.findByEngineerIdOrderByDateDesc(engineerId);
    }

    public List<LeaveRequest> getMyLeaves(UUID engineerUserId) {
        EngineerProfile engineer = engineerRepository.findByUserUserId(engineerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer profile not found"));
        return leaveRepository.findByEngineerIdOrderByAppliedAtDesc(engineer.getId());
    }

    @Transactional
    public LeaveRequest updateLeaveStatus(UUID leaveId, LeaveStatus status) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        leave.setStatus(status);
        return leaveRepository.save(leave);
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRepository.findAll();
    }
}
