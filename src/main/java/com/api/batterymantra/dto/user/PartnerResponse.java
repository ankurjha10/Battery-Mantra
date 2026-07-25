package com.api.batterymantra.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.api.batterymantra.dto.location.CityDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class PartnerResponse {
    private UUID id;
    private UUID userId;
    private String businessName;
    private String contactPerson;
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private List<CityDto> operatingCities;
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDateTime createdAt;
}
