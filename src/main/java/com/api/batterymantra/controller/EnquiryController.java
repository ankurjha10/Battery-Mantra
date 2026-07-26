package com.api.batterymantra.controller;

import com.api.batterymantra.dto.enquiry.CreateCorporateEnquiryRequest;
import com.api.batterymantra.dto.enquiry.CreateQuotationRequest;
import com.api.batterymantra.dto.enquiry.EnquiryResponse;
import com.api.batterymantra.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping("/quotation")
    public ResponseEntity<EnquiryResponse> createQuotationEnquiry(@RequestBody CreateQuotationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enquiryService.createQuotationEnquiry(request));
    }

    @PostMapping("/corporate")
    public ResponseEntity<EnquiryResponse> createCorporateEnquiry(@RequestBody CreateCorporateEnquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enquiryService.createCorporateEnquiry(request));
    }
}
