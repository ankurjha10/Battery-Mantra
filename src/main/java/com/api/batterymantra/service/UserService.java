package com.api.batterymantra.service;

import com.api.batterymantra.dto.user.UpdatePasswordRequest;
import com.api.batterymantra.dto.user.UpdateProfileRequest;
import com.api.batterymantra.dto.user.UserProfileResponse;
import com.api.batterymantra.dto.admin.AdminCreateSubAdminRequest;
import com.api.batterymantra.dto.admin.AdminUpdateSubAdminRequest;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public UserProfileResponse updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if email is taken by another user
        User existingEmailUser = userRepository.findByEmail(request.getEmail());
        if (existingEmailUser != null && !existingEmailUser.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another account");
        }

        // Check if phone number is taken by another user
        User existingPhoneUser = userRepository.findByPhoneNumber(request.getPhoneNumber());
        if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use by another account");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername().trim());
        }
        user.setEmail(request.getEmail().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());

        User updatedUser = userRepository.save(user);

        return UserProfileResponse.builder()
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .phoneNumber(updatedUser.getPhoneNumber())
                .build();
    }

    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public UserProfileResponse createSubAdmin(AdminCreateSubAdminRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }
        if (userRepository.findByPhoneNumber(request.getPhone()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already taken");
        }
        
        User user = User.builder()
                .username(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SUB_ADMIN)
                .permissions(request.getPermissions())
                .build();
                
        user = userRepository.save(user);
        
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public UserProfileResponse updateSubAdmin(UUID userId, AdminUpdateSubAdminRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub-Admin not found"));

        if (user.getRole() != UserRole.SUB_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a Sub-Admin");
        }

        User existingPhoneUser = userRepository.findByPhoneNumber(request.getPhone());
        if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use by another account");
        }

        user.setUsername(request.getName().trim());
        user.setPhoneNumber(request.getPhone().trim());
        user.setPermissions(request.getPermissions());

        user = userRepository.save(user);

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);
        
        userRepository.delete(user);
    }

    public void toggleUserStatus(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        user.setActive(isActive);
        userRepository.save(user);
    }
}
