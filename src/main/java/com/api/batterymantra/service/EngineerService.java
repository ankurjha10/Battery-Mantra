package com.api.batterymantra.service;

import com.api.batterymantra.dto.user.CreateEngineerRequest;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.EngineerProfile;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngineerService {

    private final EngineerProfileRepository engineerProfileRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EngineerResponse createEngineer(CreateEngineerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required for new engineers");
        }

        User user = new User();
        user.setUsername(request.getEmail()); // Use email as username
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ENGINEER);
        user.setActive(true);
        user = userRepository.save(user);

        PartnerProfile partnerProfile = null;
        if (request.getPartnerId() != null) {
            partnerProfile = partnerProfileRepository.findById(request.getPartnerId()).orElse(null);
        }

        EngineerProfile profile = EngineerProfile.builder()
                .user(user)
                .partnerProfile(partnerProfile)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .alternatePhone(request.getAlternatePhone())
                .address(request.getAddress())
                .city(request.getCity())
                .isActive(true)
                .build();
        
        profile = engineerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Transactional
    public EngineerResponse createPartnerEngineer(CreateEngineerRequest request, UUID partnerId) {
        request.setPartnerId(partnerId);
        return createEngineer(request);
    }

    public List<EngineerResponse> getAllEngineers() {
        return engineerProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EngineerResponse> getEngineersByPartnerId(UUID partnerId) {
        return engineerProfileRepository.findByPartnerProfileId(partnerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EngineerResponse getEngineerById(UUID id) {
        EngineerProfile profile = engineerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found"));
        return mapToResponse(profile);
    }

    @Transactional
    public EngineerResponse updateEngineer(UUID id, CreateEngineerRequest request) {
        EngineerProfile profile = engineerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found"));
        
        User user = profile.getUser();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setAlternatePhone(request.getAlternatePhone());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        
        if (request.getActive() != null) {
            profile.setActive(request.getActive());
            user.setActive(request.getActive());
            userRepository.save(user);
        }
        
        if (request.getPartnerId() != null) {
            PartnerProfile partnerProfile = partnerProfileRepository.findById(request.getPartnerId()).orElse(null);
            profile.setPartnerProfile(partnerProfile);
        }

        profile = engineerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Transactional
    public void deleteEngineer(UUID id) {
        EngineerProfile profile = engineerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found"));
        profile.setActive(false);
        profile.getUser().setActive(false);
        userRepository.save(profile.getUser());
        engineerProfileRepository.save(profile);
    }

    private EngineerResponse mapToResponse(EngineerProfile profile) {
        return EngineerResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getUser().getEmail())
                .phoneNumber(profile.getUser().getPhoneNumber())
                .alternatePhone(profile.getAlternatePhone())
                .address(profile.getAddress())
                .city(profile.getCity())
                .isActive(profile.isActive())
                .partnerId(profile.getPartnerProfile() != null ? profile.getPartnerProfile().getId() : null)
                .partnerBusinessName(profile.getPartnerProfile() != null ? profile.getPartnerProfile().getBusinessName() : null)
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
