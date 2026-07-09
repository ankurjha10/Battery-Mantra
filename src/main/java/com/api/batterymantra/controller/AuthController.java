package com.api.batterymantra.controller;

import com.api.batterymantra.dto.auth.RegisterRequest;
import com.api.batterymantra.dto.auth.LoginRequest;
import com.api.batterymantra.dto.auth.LoginResponse;
import com.api.batterymantra.dto.auth.RegisterResponse;
import com.api.batterymantra.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest user){
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest user){
        return ResponseEntity.ok(authService.login(user));
    }
}
