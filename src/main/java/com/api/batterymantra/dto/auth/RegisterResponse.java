package com.api.batterymantra.dto.auth;

import com.api.batterymantra.entity.enums.UserRole;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterResponse {
    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
    private UserRole role;
}
