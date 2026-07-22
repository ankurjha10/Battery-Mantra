package com.api.batterymantra.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEngineerRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phoneNumber;

    private String alternatePhone;

    private String address;
    private String city;

    private String password;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive;

    private UUID partnerId;
}
