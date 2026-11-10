package com.api.batterymantra.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGateConfigResponse {
    private boolean adminPinConfigured;
    private boolean subAdminPinConfigured;
    private LocalDateTime updatedAt;
    private List<UserGatePinStatus> users;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGatePinStatus {
        private UUID userId;
        private String username;
        private String email;
        private String role;
        private boolean isActive;
        private boolean personalPinConfigured;
        private LocalDateTime gatePinUpdatedAt;
    }
}
