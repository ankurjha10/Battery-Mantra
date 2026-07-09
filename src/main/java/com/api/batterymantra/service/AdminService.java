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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;


    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
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
