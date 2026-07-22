package com.api.batterymantra.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePartnerRequest {
    @NotBlank
    private String businessName;
    
    @NotBlank
    private String contactPerson;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phoneNumber;

    private String alternatePhone;
    
    private String address;

    @NotBlank
    private String password;
    
    private List<UUID> operatingCityIds;
}
