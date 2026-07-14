package com.api.batterymantra.service;

import com.api.batterymantra.entity.RefreshToken;
import com.api.batterymantra.repository.RefreshTokenRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Long REFRESH_TOKEN_VALIDITY = Duration.ofHours(12).toMillis();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken createRefreshToken(String userName) {
        com.api.batterymantra.entity.User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        refreshTokenRepository.findByUser(user).ifPresent(existing -> {
            refreshTokenRepository.delete(existing);
            refreshTokenRepository.flush();
        });

        RefreshToken token = RefreshToken.builder()
                .refreshToken(UUID.randomUUID().toString())
                .expiry(Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY))
                .user(user)
                .build();

        return refreshTokenRepository.save(token);
    }

    public RefreshToken verifyRefreshToken(String refreshToken) {

        RefreshToken refreshToken1 = refreshTokenRepository.findById(refreshToken).orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (Instant.now().isAfter(refreshToken1.getExpiry())) {
            refreshTokenRepository.delete(refreshToken1);
            throw new RuntimeException("Refresh token expired");
        }
        return refreshToken1;
    }
}
