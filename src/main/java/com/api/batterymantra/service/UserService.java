package com.api.batterymantra.service;

import com.api.batterymantra.dto.user.UpdatePasswordRequest;
import com.api.batterymantra.dto.user.UpdateProfileRequest;
import com.api.batterymantra.dto.user.UserProfileResponse;
import com.api.batterymantra.dto.admin.AdminCreateSubAdminRequest;
import com.api.batterymantra.dto.admin.AdminUpdateSubAdminRequest;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final CallLogRepository callLogRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final NotificationRepository notificationRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final AddressRepository addressRepository;
    private final EngineerProfileRepository engineerProfileRepository;
    private final PartnerProfileRepository partnerProfileRepository;

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
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        String trimmedPhone = request.getPhone() != null ? request.getPhone().trim() : "";
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (userRepository.findByEmail(trimmedEmail) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }
        if (userRepository.findByPhoneNumber(trimmedPhone) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already taken");
        }
        if (userRepository.findByUsername(trimmedName) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Name/Username is already taken");
        }
        
        User user = User.builder()
                .username(trimmedName)
                .email(trimmedEmail)
                .phoneNumber(trimmedPhone)
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

        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        String trimmedPhone = request.getPhone() != null ? request.getPhone().trim() : "";

        User existingPhoneUser = userRepository.findByPhoneNumber(trimmedPhone);
        if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use by another account");
        }

        User existingUsernameUser = userRepository.findByUsername(trimmedName);
        if (existingUsernameUser != null && !existingUsernameUser.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Name/Username is already in use by another account");
        }

        user.setUsername(trimmedName);
        user.setPhoneNumber(trimmedPhone);
        user.setPermissions(request.getPermissions());

        user = userRepository.save(user);

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 1. Delete RefreshToken
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        // 2. Delete Cart & CartItems (Cart has CascadeType.ALL, orphanRemoval = true on cartItems)
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);

        // 3. Delete Wishlist items
        List<Wishlist> wishlists = wishlistRepository.findByUserUserId(userId);
        if (!wishlists.isEmpty()) {
            wishlistRepository.deleteAll(wishlists);
        }

        // 4. Delete Notifications
        notificationRepository.deleteAllByUserUserId(userId);

        // 5. Delete User Vehicles
        List<UserVehicle> vehicles = userVehicleRepository.findByUserUserId(userId);
        if (!vehicles.isEmpty()) {
            userVehicleRepository.deleteAll(vehicles);
        }

        // 6. Delete Engineer / Partner Profiles if any
        engineerProfileRepository.findByUserUserId(userId).ifPresent(eng -> {
            List<CallLog> engCallLogs = callLogRepository.findByEngineerId(eng.getId());
            if (!engCallLogs.isEmpty()) {
                callLogRepository.deleteAll(engCallLogs);
            }
            List<Orders> assignedOrders = orderRepository.findByAssignedEngineer_IdOrderByPlacedAtDesc(eng.getId());
            for (Orders o : assignedOrders) {
                o.setAssignedEngineer(null);
                orderRepository.save(o);
            }
            engineerProfileRepository.delete(eng);
        });

        partnerProfileRepository.findByUserUserId(userId).ifPresent(partner -> {
            List<Orders> assignedOrders = orderRepository.findByAssignedPartner_IdOrderByPlacedAtDesc(partner.getId());
            for (Orders o : assignedOrders) {
                o.setAssignedPartner(null);
                orderRepository.save(o);
            }
            partnerProfileRepository.delete(partner);
        });

        // 7. Delete Orders & their dependencies (CallLogs, OrderItems)
        List<Orders> orders = orderRepository.findByCustomer_UserIdOrderByPlacedAtDesc(userId);
        for (Orders order : orders) {
            // Delete any CallLogs linked to this order
            List<CallLog> callLogs = callLogRepository.findByOrderOrderId(order.getOrderId());
            if (!callLogs.isEmpty()) {
                callLogRepository.deleteAll(callLogs);
            }
            // orderRepository.delete cascades to OrderItems (CascadeType.ALL, orphanRemoval = true)
            orderRepository.delete(order);
        }

        // 8. Delete Addresses (Safe now because all orders referencing shippingAddress are deleted)
        List<Address> addresses = addressRepository.findByUserUserId(userId);
        if (!addresses.isEmpty()) {
            addressRepository.deleteAll(addresses);
        }

        // 9. Finally delete User entity
        userRepository.delete(user);
    }

    public void toggleUserStatus(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        user.setActive(isActive);
        userRepository.save(user);
    }
}
