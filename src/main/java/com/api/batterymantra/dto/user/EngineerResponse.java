package com.api.batterymantra.dto.user;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class EngineerResponse {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String city;
    private boolean isActive;
    private UUID partnerId;
    private String partnerBusinessName;
    private LocalDateTime createdAt;
}
