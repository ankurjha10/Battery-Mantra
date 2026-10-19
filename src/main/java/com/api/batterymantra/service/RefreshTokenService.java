package com.api.batterymantra.service;

import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.RefreshToken;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.exception.TokenRefreshException;
import com.api.batterymantra.repository.RefreshTokenRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshValidityMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(String userName) {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with username: " + userName);
        }

        refreshTokenRepository.findByUser(user).ifPresent(existing -> {
            refreshTokenRepository.delete(existing);
            refreshTokenRepository.flush();
        });

        RefreshToken token = RefreshToken.builder()
                .refreshToken(UUID.randomUUID().toString())
                .expiry(Instant.now().plusMillis(refreshValidityMs))
                .user(user)
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new TokenRefreshException("Refresh token is required");
        }

        RefreshToken token = refreshTokenRepository.findById(refreshToken.trim())
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found or already invalidated"));

        if (Instant.now().isAfter(token.getExpiry())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token expired. Please log in again");
        }
        return token;
    }
}

