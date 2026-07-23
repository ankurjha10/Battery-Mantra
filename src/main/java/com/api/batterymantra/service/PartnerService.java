package com.api.batterymantra.service;

import com.api.batterymantra.dto.location.CityDto;
import com.api.batterymantra.dto.user.CreatePartnerRequest;
import com.api.batterymantra.dto.user.PartnerResponse;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PartnerResponse createPartner(CreatePartnerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required for new partners");
        }

        User user = new User();
        user.setUsername(request.getEmail()); // Use email as username
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.PARTNER);
        user.setActive(true);
        user = userRepository.save(user);

        Set<City> cities = new HashSet<>();
        if (request.getOperatingCityIds() != null) {
            cities = new HashSet<>(cityRepository.findAllById(request.getOperatingCityIds()));
        }

        PartnerProfile profile = PartnerProfile.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .contactPerson(request.getContactPerson())
                .alternatePhone(request.getAlternatePhone())
                .address(request.getAddress())
                .operatingCities(cities)
                .isActive(true)
                .build();
        
        profile = partnerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    public List<PartnerResponse> getAllPartners() {
        return partnerProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartnerResponse getPartnerById(UUID id) {
        PartnerProfile profile = partnerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        return mapToResponse(profile);
    }

    @Transactional
    public PartnerResponse updatePartner(UUID id, CreatePartnerRequest request) {
        PartnerProfile profile = partnerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        
        User user = profile.getUser();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        Set<City> cities = new HashSet<>();
        if (request.getOperatingCityIds() != null) {
            cities = new HashSet<>(cityRepository.findAllById(request.getOperatingCityIds()));
        }

        profile.setBusinessName(request.getBusinessName());
        profile.setContactPerson(request.getContactPerson());
        profile.setAlternatePhone(request.getAlternatePhone());
        profile.setAddress(request.getAddress());
        profile.setOperatingCities(cities);
        
        if (request.getActive() != null) {
            profile.setActive(request.getActive());
            user.setActive(request.getActive());
            userRepository.save(user);
        }
        
        profile = partnerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Transactional
    public void deletePartner(UUID id) {
        PartnerProfile profile = partnerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        profile.setActive(false);
        profile.getUser().setActive(false);
        userRepository.save(profile.getUser());
        partnerProfileRepository.save(profile);
    }

    private PartnerResponse mapToResponse(PartnerProfile profile) {
        return PartnerResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getUserId())
                .businessName(profile.getBusinessName())
                .contactPerson(profile.getContactPerson())
                .email(profile.getUser().getEmail())
                .phoneNumber(profile.getUser().getPhoneNumber())
                .alternatePhone(profile.getAlternatePhone())
                .address(profile.getAddress())
                .operatingCities(profile.getOperatingCities().stream().map(c -> CityDto.builder().cityId(c.getCityId()).cityName(c.getCityName()).stateName(c.getStateName()).isPopular(c.getIsPopular()).isCodAvailable(c.getIsCodAvailable()).isExchangeAvailable(c.getIsExchangeAvailable()).build()).collect(Collectors.toList()))
                .isActive(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
