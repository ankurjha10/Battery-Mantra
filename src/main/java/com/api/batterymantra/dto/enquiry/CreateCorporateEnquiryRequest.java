package com.api.batterymantra.dto.enquiry;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateCorporateEnquiryRequest {
    private String companyName;
    private String contactPerson;
    private String mobileNumber;
    private String email;
    private String gstin;
    private String estimatedQty;
    private String notes;
    private UUID productId;
    private String productName;
}
