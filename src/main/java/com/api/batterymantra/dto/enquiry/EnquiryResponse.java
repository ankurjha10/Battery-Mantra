package com.api.batterymantra.dto.enquiry;

import com.api.batterymantra.entity.enums.EnquiryStatus;
import com.api.batterymantra.entity.enums.EnquiryType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EnquiryResponse {
    private Long id;
    private EnquiryType enquiryType;
    private String name;
    private String mobileNumber;
    private String email;
    private String companyName;
    private String gstin;
    private String quantity;
    private String message;
    private UUID productId;
    private String productName;
    private EnquiryStatus status;
    private LocalDateTime createdAt;
}
