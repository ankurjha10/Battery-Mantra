package com.api.batterymantra.dto.enquiry;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateQuotationRequest {
    private String name;
    private String mobileNumber;
    private String email;
    private String quantity;
    private String message;
    private UUID productId;
    private String productName;
}
