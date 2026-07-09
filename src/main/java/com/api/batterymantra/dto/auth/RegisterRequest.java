package com.api.batterymantra.dto.auth;

import com.api.batterymantra.entity.enums.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private UserRole role;
}
