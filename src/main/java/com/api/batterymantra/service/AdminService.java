package com.api.batterymantra.service;

import com.api.batterymantra.dto.admin.UserResponse;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.batterymantra.dto.admin.AdminCreateCustomerRequest;
import com.api.batterymantra.dto.admin.AdminUpdateCustomerRequest;
import com.api.batterymantra.entity.Address;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.api.batterymantra.repository.AddressRepository;
import com.api.batterymantra.entity.enums.UserRole;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public UserResponse createCustomer(AdminCreateCustomerRequest request) {
        if (userRepository.existsByEmailOrPhoneNumber(request.getEmail(), request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists with this phone or email");
        }

        User newUser = User.builder()
                .username(request.getName())
                .email(request.getEmail() != null ? request.getEmail() : "")
                .phoneNumber(request.getPhone())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);

        Address address = Address.builder()
                .user(savedUser)
                .fullName(request.getName())
                .phoneNumber(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPincode())
                .country("India")
                .isDefault(true)
                .build();

        addressRepository.save(address);

        return toUserResponse(savedUser);
    }

    public UserResponse updateCustomer(UUID userId, AdminUpdateCustomerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + userId));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setUsername(request.getName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String newPhone = request.getPhone().trim();
            User existingWithPhone = userRepository.findByPhoneNumber(newPhone);
            if (existingWithPhone != null && !existingWithPhone.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use by another account");
            }
            user.setPhoneNumber(newPhone);
        }

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.isEmpty()) {
                User existingWithEmail = userRepository.findByEmail(newEmail);
                if (existingWithEmail != null && !existingWithEmail.getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another account");
                }
            }
            user.setEmail(newEmail);
        }

        User savedUser = userRepository.save(user);

        // Update default address if address fields are provided
        if (request.getAddressLine1() != null || request.getCity() != null || request.getPincode() != null) {
            List<Address> addresses = addressRepository.findAllByUserUserIdAndIsDeletedFalse(userId);
            Address defaultAddress = addresses.stream()
                    .filter(Address::isDefault)
                    .findFirst()
                    .orElse(addresses.isEmpty() ? null : addresses.get(0));

            if (defaultAddress == null) {
                defaultAddress = Address.builder()
                        .user(savedUser)
                        .country("India")
                        .isDefault(true)
                        .build();
            }

            defaultAddress.setFullName(savedUser.getUsername());
            defaultAddress.setPhoneNumber(savedUser.getPhoneNumber());
            if (request.getAddressLine1() != null && !request.getAddressLine1().isBlank()) {
                defaultAddress.setAddressLine1(request.getAddressLine1().trim());
            }
            if (request.getAddressLine2() != null) {
                defaultAddress.setAddressLine2(request.getAddressLine2().trim());
            }
            if (request.getCity() != null && !request.getCity().isBlank()) {
                defaultAddress.setCity(request.getCity().trim());
            }
            if (request.getState() != null && !request.getState().isBlank()) {
                defaultAddress.setState(request.getState().trim());
            }
            if (request.getPincode() != null && !request.getPincode().isBlank()) {
                defaultAddress.setPostalCode(request.getPincode().trim());
            }

            addressRepository.save(defaultAddress);
        }

        return toUserResponse(savedUser);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .permissions(user.getPermissions())
                .build();
    }
}
