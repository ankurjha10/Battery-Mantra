package com.api.batterymantra.service;

import com.api.batterymantra.dto.vehicle.UserVehicleRequest;
import com.api.batterymantra.dto.vehicle.UserVehicleResponse;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserVehicle;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserVehicleService {

    private final UserVehicleRepository userVehicleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserVehicleResponse> getUserVehicles(UUID userId) {
        List<UserVehicle> vehicles = userVehicleRepository.findByUserUserId(userId);
        return vehicles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserVehicleResponse addUserVehicle(UUID userId, UserVehicleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserVehicle vehicle = UserVehicle.builder()
                .user(user)
                .vehicleType(request.getVehicleType())
                .manufacturer(request.getManufacturer())
                .modelName(request.getModelName())
                .fuelType(request.getFuelType())
                .nickname(request.getNickname())
                .build();

        UserVehicle savedVehicle = userVehicleRepository.save(vehicle);
        return mapToResponse(savedVehicle);
    }

    @Transactional
    public void deleteUserVehicle(UUID userId, UUID vehicleId) {
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("User vehicle not found with id: " + vehicleId));

        if (!vehicle.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("User vehicle does not belong to the user");
        }

        userVehicleRepository.delete(vehicle);
    }

    private UserVehicleResponse mapToResponse(UserVehicle vehicle) {
        UserVehicleResponse response = new UserVehicleResponse();
        response.setId(vehicle.getId());
        response.setVehicleType(vehicle.getVehicleType());
        response.setManufacturer(vehicle.getManufacturer());
        response.setModelName(vehicle.getModelName());
        response.setFuelType(vehicle.getFuelType());
        response.setNickname(vehicle.getNickname());
        response.setCreatedAt(vehicle.getCreatedAt());
        return response;
    }
}
