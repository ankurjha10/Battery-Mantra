package com.api.batterymantra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.batterymantra.dto.auth.LoginRequest;
import com.api.batterymantra.dto.auth.LoginResponse;
import com.api.batterymantra.dto.auth.RefreshTokenRequest;
import com.api.batterymantra.dto.auth.RefreshTokenResponse;
import com.api.batterymantra.dto.auth.RegisterRequest;
import com.api.batterymantra.dto.auth.RegisterResponse;
import com.api.batterymantra.entity.RefreshToken;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.security.AuthUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final AuthenticationManager authManager;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public RegisterResponse register(RegisterRequest signUpRequest) {
        // Validate input
        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (signUpRequest.getEmail() == null || signUpRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (signUpRequest.getPassword() == null || signUpRequest.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (signUpRequest.getPhoneNumber() == null || signUpRequest.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        // Check if user already exists
        boolean userExists = userRepository.existsByEmailOrPhoneNumber(
                signUpRequest.getEmail().trim(),
                signUpRequest.getPhoneNumber().trim()
        );

        if (userExists) {
            throw new IllegalArgumentException("User already exists with this email or phone number");
        }

        // Check if username already taken
        if (userRepository.findByUsername(signUpRequest.getUsername().trim()) != null) {
            throw new IllegalArgumentException("Username already taken");
        }

        try {
            User newUser = User.builder()
                    .username(signUpRequest.getUsername().trim())
                    .email(signUpRequest.getEmail().trim())
                    .password(passwordEncoder.encode(signUpRequest.getPassword()))
                    .phoneNumber(signUpRequest.getPhoneNumber().trim())
                    .role(signUpRequest.getRole() != null ? signUpRequest.getRole() : UserRole.CUSTOMER)
                    .isActive(true)
                    .build();


            User savedUser = userRepository.save(newUser);

            RegisterResponse res = new RegisterResponse();
            res.setId(savedUser.getUserId());
            res.setUsername(savedUser.getUsername());
            res.setEmail(savedUser.getEmail());
            res.setPhoneNumber(savedUser.getPhoneNumber());
            res.setRole(savedUser.getRole());

            return res;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error during user registration: " + e.getMessage());
        }
    }

    public LoginResponse login(LoginRequest loginRequest) {
        System.out.println("LOGIN METHOD HIT");

        try {
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }

            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername().trim(),
                            loginRequest.getPassword()
                    )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            String token = authUtil.generateAccessToken(user);
            
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

            return new LoginResponse(token, refreshToken.getRefreshToken(), user.getUserId(), user.getRole().name());
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid username or password");
        } catch (UsernameNotFoundException e) {
            throw new IllegalArgumentException("User not found");
        } catch (Exception e) {
            throw new IllegalArgumentException("Login failed: " + e.getMessage());
        }
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        String accessToken = authUtil.generateAccessToken(user);
        
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .build();
    }
}
