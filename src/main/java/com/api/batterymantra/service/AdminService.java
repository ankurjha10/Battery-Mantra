package com.api.batterymantra.service;

import com.api.batterymantra.dto.admin.UserResponse;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.batterymantra.dto.admin.AdminCreateCustomerRequest;
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
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public List<UserResponse> getAllUsers(){
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

    private UserResponse toUserResponse(User user){
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
