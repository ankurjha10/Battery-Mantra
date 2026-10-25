package com.api.batterymantra.dto.admin;

import lombok.Data;

@Data
public class AdminUpdateCustomerRequest {
    private String name;
    private String phone;
    private String email;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
}
