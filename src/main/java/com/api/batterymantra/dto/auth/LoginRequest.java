package com.api.batterymantra.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    public String username;
    public String password;
}
