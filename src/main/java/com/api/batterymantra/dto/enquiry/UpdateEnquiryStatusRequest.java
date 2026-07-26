package com.api.batterymantra.dto.enquiry;

import com.api.batterymantra.entity.enums.EnquiryStatus;
import lombok.Data;

@Data
public class UpdateEnquiryStatusRequest {
    private EnquiryStatus status;
}
