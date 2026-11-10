package com.api.batterymantra.service;

import com.api.batterymantra.dto.auth.AdminGateConfigResponse;
import com.api.batterymantra.dto.auth.AdminGateRequest;
import com.api.batterymantra.dto.auth.AdminGateResponse;
import com.api.batterymantra.dto.auth.UpdateGatePinRequest;
import com.api.batterymantra.entity.AdminGateConfig;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.AdminGateConfigRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminGateService {

    private final AdminGateConfigRepository adminGateConfigRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;

    @Value("${app.security.admin-gate-pin:849201}")
    private String defaultAdminPin = "849201";

    @Value("${app.security.sub-admin-gate-pin:517309}")
    private String defaultSubAdminPin = "517309";

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultGateConfigOnStartup() {
        try {
            getOrInitializeConfig();
        } catch (Exception e) {
            log.warn("Could not pre-initialize AdminGateConfig on startup: {}", e.getMessage());
        }
    }

    @Transactional
    public AdminGateResponse verifyGatePin(AdminGateRequest request) {
        if (request == null || request.getPin() == null || request.getPin().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid gate PIN");
        }

        String candidatePin = request.getPin().trim();

        // 1. Check per-user personal gate PINs across active ADMIN and SUB_ADMIN users
        for (User staffUser : findAllAdminAndSubAdminUsers()) {
            if (staffUser.isActive() && staffUser.getGatePinHash() != null && !staffUser.getGatePinHash().isBlank()) {
                if (passwordEncoder.matches(candidatePin, staffUser.getGatePinHash())) {
                    String tier = staffUser.getRole().name();
                    return AdminGateResponse.builder()
                            .tier(tier)
                            .gateToken(authUtil.generateGateToken(tier, staffUser.getUserId()))
                            .build();
                }
            }
        }

        // 2. Fallback: Check tier-level default gate PINs
        AdminGateConfig config = getOrInitializeConfig();

        if (config.getAdminPinHash() != null && passwordEncoder.matches(candidatePin, config.getAdminPinHash())) {
            return AdminGateResponse.builder()
                    .tier("ADMIN")
                    .gateToken(authUtil.generateGateToken("ADMIN", null))
                    .build();
        }

        if (config.getSubAdminPinHash() != null && passwordEncoder.matches(candidatePin, config.getSubAdminPinHash())) {
            return AdminGateResponse.builder()
                    .tier("SUB_ADMIN")
                    .gateToken(authUtil.generateGateToken("SUB_ADMIN", null))
                    .build();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid gate PIN");
    }

    @Transactional
    public AdminGateConfigResponse getGateConfig() {
        AdminGateConfig config = getOrInitializeConfig();
        return toConfigResponse(config);
    }

    @Transactional
    public AdminGateConfigResponse updateGatePin(UUID adminUserId, UpdateGatePinRequest request) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Super Admin (ROLE_ADMIN) can modify gate PINs");
        }

        if (request.getCurrentPassword() == null ||
                !passwordEncoder.matches(request.getCurrentPassword(), adminUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        String newPin = request.getNewPin() != null ? request.getNewPin().trim() : "";
        if (newPin.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gate PIN must be at least 4 characters");
        }

        AdminGateConfig config = getOrInitializeConfig();

        if (request.getTargetUserId() != null) {
            User targetUser = userRepository.findById(request.getTargetUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Target user not found: " + request.getTargetUserId()));

            if (targetUser.getRole() != UserRole.ADMIN && targetUser.getRole() != UserRole.SUB_ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Gate PINs can only be assigned to ADMIN or SUB_ADMIN users");
            }

            ensurePinUniqueForUser(newPin, targetUser.getUserId(), config);

            targetUser.setGatePinHash(passwordEncoder.encode(newPin));
            targetUser.setGatePinUpdatedAt(LocalDateTime.now());
            userRepository.save(targetUser);

            config.setUpdatedAt(LocalDateTime.now());
            AdminGateConfig savedConfig = adminGateConfigRepository.save(config);
            log.info("Personal gate PIN updated for {} '{}' by Super Admin '{}'",
                    targetUser.getRole(), targetUser.getUsername(), adminUser.getUsername());
            return toConfigResponse(savedConfig);
        }

        String targetRole = request.getTargetRole() != null ? request.getTargetRole().trim().toUpperCase() : "";
        if (!"ADMIN".equals(targetRole) && !"SUB_ADMIN".equals(targetRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either targetUserId or targetRole (ADMIN or SUB_ADMIN) is required");
        }

        ensurePinUniqueForTier(newPin, targetRole, config);

        if ("ADMIN".equals(targetRole)) {
            config.setAdminPinHash(passwordEncoder.encode(newPin));
        } else {
            config.setSubAdminPinHash(passwordEncoder.encode(newPin));
        }

        config.setUpdatedAt(LocalDateTime.now());
        AdminGateConfig saved = adminGateConfigRepository.save(config);
        log.info("Tier fallback gate PIN updated for tier {} by Super Admin '{}'", targetRole, adminUser.getUsername());
        return toConfigResponse(saved);
    }

    public void validateNewUserGatePin(String newPin) {
        if (newPin == null || newPin.isBlank()) {
            return;
        }
        String trimmed = newPin.trim();
        if (trimmed.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gate PIN must be at least 4 characters");
        }
        AdminGateConfig config = getOrInitializeConfig();
        ensurePinUniqueForUser(trimmed, null, config);
    }

    private void ensurePinUniqueForUser(String rawPin, UUID excludeUserId, AdminGateConfig config) {
        for (User existing : findAllAdminAndSubAdminUsers()) {
            if (excludeUserId != null && excludeUserId.equals(existing.getUserId())) {
                continue;
            }
            if (existing.getGatePinHash() != null && !existing.getGatePinHash().isBlank() &&
                    passwordEncoder.matches(rawPin, existing.getGatePinHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Gate PIN must be unique across all Admin and Sub-Admin accounts");
            }
        }
        if ((config.getAdminPinHash() != null && passwordEncoder.matches(rawPin, config.getAdminPinHash())) ||
                (config.getSubAdminPinHash() != null && passwordEncoder.matches(rawPin, config.getSubAdminPinHash()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Personal gate PIN must be distinct from the default ADMIN/SUB_ADMIN gate PINs");
        }
    }

    private void ensurePinUniqueForTier(String rawPin, String targetRole, AdminGateConfig config) {
        if ("ADMIN".equals(targetRole) && config.getSubAdminPinHash() != null &&
                passwordEncoder.matches(rawPin, config.getSubAdminPinHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ADMIN gate PIN must be distinct from SUB_ADMIN gate PIN");
        }
        if ("SUB_ADMIN".equals(targetRole) && config.getAdminPinHash() != null &&
                passwordEncoder.matches(rawPin, config.getAdminPinHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "SUB_ADMIN gate PIN must be distinct from ADMIN gate PIN");
        }
        for (User existing : findAllAdminAndSubAdminUsers()) {
            if (existing.getGatePinHash() != null && !existing.getGatePinHash().isBlank() &&
                    passwordEncoder.matches(rawPin, existing.getGatePinHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tier gate PIN must be distinct from existing personal Admin/Sub-Admin PINs");
            }
        }
    }

    private List<User> findAllAdminAndSubAdminUsers() {
        List<User> staff = new ArrayList<>();
        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
        if (admins != null) {
            staff.addAll(admins);
        }
        List<User> subAdmins = userRepository.findAllByRole(UserRole.SUB_ADMIN);
        if (subAdmins != null) {
            staff.addAll(subAdmins);
        }
        return staff;
    }

    private AdminGateConfig getOrInitializeConfig() {
        return adminGateConfigRepository.findById(AdminGateConfig.SINGLETON_ID)
                .map(existing -> {
                    boolean modified = false;
                    if (existing.getAdminPinHash() == null || existing.getAdminPinHash().isBlank()) {
                        existing.setAdminPinHash(passwordEncoder.encode(resolveAdminDefaultPin()));
                        modified = true;
                    }
                    if (existing.getSubAdminPinHash() == null || existing.getSubAdminPinHash().isBlank()) {
                        existing.setSubAdminPinHash(passwordEncoder.encode(resolveSubAdminDefaultPin()));
                        modified = true;
                    }
                    return modified ? adminGateConfigRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    String adminPin = resolveAdminDefaultPin();
                    String subAdminPin = resolveSubAdminDefaultPin();
                    if (adminPin.equals(subAdminPin)) {
                        throw new IllegalStateException("Default ADMIN and SUB_ADMIN gate PINs must be distinct");
                    }
                    AdminGateConfig created = AdminGateConfig.builder()
                            .id(AdminGateConfig.SINGLETON_ID)
                            .adminPinHash(passwordEncoder.encode(adminPin))
                            .subAdminPinHash(passwordEncoder.encode(subAdminPin))
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return adminGateConfigRepository.save(created);
                });
    }

    private String resolveAdminDefaultPin() {
        return (defaultAdminPin != null && !defaultAdminPin.isBlank()) ? defaultAdminPin.trim() : "849201";
    }

    private String resolveSubAdminDefaultPin() {
        return (defaultSubAdminPin != null && !defaultSubAdminPin.isBlank()) ? defaultSubAdminPin.trim() : "517309";
    }

    private AdminGateConfigResponse toConfigResponse(AdminGateConfig config) {
        List<AdminGateConfigResponse.UserGatePinStatus> users = findAllAdminAndSubAdminUsers().stream()
                .map(u -> AdminGateConfigResponse.UserGatePinStatus.builder()
                        .userId(u.getUserId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .role(u.getRole() != null ? u.getRole().name() : null)
                        .isActive(u.isActive())
                        .personalPinConfigured(u.getGatePinHash() != null && !u.getGatePinHash().isBlank())
                        .gatePinUpdatedAt(u.getGatePinUpdatedAt())
                        .build())
                .toList();

        return AdminGateConfigResponse.builder()
                .adminPinConfigured(config.getAdminPinHash() != null && !config.getAdminPinHash().isBlank())
                .subAdminPinConfigured(config.getSubAdminPinHash() != null && !config.getSubAdminPinHash().isBlank())
                .updatedAt(config.getUpdatedAt())
                .users(users)
                .build();
    }
}
