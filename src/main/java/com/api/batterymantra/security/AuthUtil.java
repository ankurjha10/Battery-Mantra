package com.api.batterymantra.security;

import com.api.batterymantra.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class AuthUtil {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId().toString())
                .claim("role", "ROLE_" + user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }

    public record GateTokenClaims(String tier, UUID targetUserId) {}

    public String generateGateToken(String tier) {
        return generateGateToken(tier, null);
    }

    public String generateGateToken(String tier, UUID targetUserId) {
        long gateExpirationMs = 5 * 60 * 1000L; // 5 minutes
        var builder = Jwts.builder()
                .subject("ADMIN_GATE")
                .claim("type", "ADMIN_GATE")
                .claim("tier", tier)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + gateExpirationMs));
        if (targetUserId != null) {
            builder.claim("targetUserId", targetUserId.toString());
        }
        return builder.signWith(getSecretKey()).compact();
    }

    public String validateAndExtractGateTier(String gateToken) {
        return validateAndExtractGateClaims(gateToken).tier();
    }

    public GateTokenClaims validateAndExtractGateClaims(String gateToken) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(gateToken)
                .getPayload();
        String type = claims.get("type", String.class);
        if (!"ADMIN_GATE".equals(type)) {
            throw new IllegalArgumentException("Invalid gate token type");
        }
        String tier = claims.get("tier", String.class);
        if (!"ADMIN".equals(tier) && !"SUB_ADMIN".equals(tier)) {
            throw new IllegalArgumentException("Invalid gate token tier");
        }
        String targetUserIdStr = claims.get("targetUserId", String.class);
        UUID targetUserId = (targetUserIdStr != null && !targetUserIdStr.isBlank())
                ? UUID.fromString(targetUserIdStr)
                : null;
        return new GateTokenClaims(tier, targetUserId);
    }
}
