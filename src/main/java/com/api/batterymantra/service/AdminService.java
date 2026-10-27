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

    @org.springframework.transaction.annotation.Transactional
    public UserResponse createCustomer(AdminCreateCustomerRequest request) {
        String trimmedPhone = request.getPhone() != null ? request.getPhone().trim() : "";
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        String rawEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (userRepository.findByPhoneNumber(trimmedPhone) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already exists with phone number: " + trimmedPhone);
        }

        if (!rawEmail.isEmpty() && userRepository.findByEmail(rawEmail) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already exists with email: " + rawEmail);
        }

        // Fallback to phone@batterymantra.com matching AuthService OTP auto-registration convention
        String email = !rawEmail.isEmpty() ? rawEmail : (trimmedPhone + "@batterymantra.com");

        String username = trimmedName;
        if (userRepository.findByUsername(username) != null) {
            username = trimmedName + "_" + trimmedPhone;
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .phoneNumber(trimmedPhone)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);

        Address address = Address.builder()
                .user(savedUser)
                .fullName(trimmedName)
                .phoneNumber(trimmedPhone)
                .addressLine1(request.getAddressLine1() != null ? request.getAddressLine1().trim() : "")
                .addressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : "")
                .state(request.getState() != null ? request.getState().trim() : "")
                .postalCode(request.getPincode() != null ? request.getPincode().trim() : "")
                .country("India")
                .isDefault(true)
                .build();

        addressRepository.save(address);

        return toUserResponse(savedUser);
    }

    @org.springframework.transaction.annotation.Transactional
    public UserResponse updateCustomer(UUID userId, AdminUpdateCustomerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + userId));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            User existingWithUsername = userRepository.findByUsername(newName);
            if (existingWithUsername != null && !existingWithUsername.getUserId().equals(userId)) {
                newName = newName + "_" + user.getPhoneNumber();
            }
            user.setUsername(newName);
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
                user.setEmail(newEmail);
            }
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
