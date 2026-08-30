package com.api.batterymantra.controller;

import com.api.batterymantra.dto.auth.RefreshTokenRequest;
import com.api.batterymantra.dto.auth.RefreshTokenResponse;
import com.api.batterymantra.dto.auth.RegisterRequest;
import com.api.batterymantra.dto.auth.LoginRequest;
import com.api.batterymantra.dto.auth.LoginResponse;
import com.api.batterymantra.dto.auth.RegisterResponse;
import com.api.batterymantra.dto.auth.SendOtpRequest;
import com.api.batterymantra.dto.auth.VerifyOtpRequest;
import com.api.batterymantra.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.api.batterymantra.dto.auth.CheckUserResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-user")
    public ResponseEntity<CheckUserResponse> checkUser(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(authService.checkUserByPhone(phoneNumber));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest user) {
        return ResponseEntity.ok(authService.login(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }
}
