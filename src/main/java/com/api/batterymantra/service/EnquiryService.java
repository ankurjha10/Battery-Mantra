package com.api.batterymantra.service;

import com.api.batterymantra.dto.enquiry.CreateCorporateEnquiryRequest;
import com.api.batterymantra.dto.enquiry.CreateQuotationRequest;
import com.api.batterymantra.dto.enquiry.EnquiryResponse;
import com.api.batterymantra.entity.EnquiryRequest;
import com.api.batterymantra.entity.enums.EnquiryStatus;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.entity.enums.EnquiryType;
import com.api.batterymantra.repository.EnquiryRequestRepository;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRequestRepository enquiryRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private EnquiryResponse toResponse(EnquiryRequest entity) {
        EnquiryResponse response = new EnquiryResponse();
        response.setId(entity.getId());
        response.setEnquiryType(entity.getEnquiryType());
        response.setName(entity.getName());
        response.setMobileNumber(entity.getMobileNumber());
        response.setEmail(entity.getEmail());
        response.setCompanyName(entity.getCompanyName());
        response.setGstin(entity.getGstin());
        response.setQuantity(entity.getQuantity());
        response.setMessage(entity.getMessage());
        response.setProductId(entity.getProductId());
        response.setProductName(entity.getProductName());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    @Transactional
    public EnquiryResponse createQuotationEnquiry(CreateQuotationRequest request) {
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }

        EnquiryRequest enquiry = new EnquiryRequest();
        enquiry.setEnquiryType(EnquiryType.QUOTATION);
        enquiry.setName(request.getName());
        enquiry.setMobileNumber(request.getMobileNumber());
        enquiry.setEmail(request.getEmail());
        enquiry.setQuantity(request.getQuantity());
        enquiry.setMessage(request.getMessage());
        enquiry.setProductId(request.getProductId());
        enquiry.setProductName(request.getProductName());
        enquiry.setStatus(EnquiryStatus.PENDING);

        EnquiryRequest saved = enquiryRequestRepository.save(enquiry);

        try {
            List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
            for (User admin : admins) {
                notificationService.sendPushNotification(
                        admin.getUserId(),
                        "New Enquiry Received",
                        "New enquiry from " + request.getName() + " regarding " + request.getProductName() + ".",
                        null
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send push notification for enquiry: " + e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public EnquiryResponse createCorporateEnquiry(CreateCorporateEnquiryRequest request) {
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }

        EnquiryRequest enquiry = new EnquiryRequest();
        enquiry.setEnquiryType(EnquiryType.CORPORATE);
        enquiry.setName(request.getContactPerson());
        enquiry.setMobileNumber(request.getMobileNumber());
        enquiry.setEmail(request.getEmail());
        enquiry.setCompanyName(request.getCompanyName());
        enquiry.setGstin(request.getGstin());
        enquiry.setQuantity(request.getEstimatedQty());
        enquiry.setMessage(request.getNotes());
        enquiry.setProductId(request.getProductId());
        enquiry.setProductName(request.getProductName());
        enquiry.setStatus(EnquiryStatus.PENDING);

        EnquiryRequest saved = enquiryRequestRepository.save(enquiry);

        try {
            List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
            for (User admin : admins) {
                notificationService.sendPushNotification(
                        admin.getUserId(),
                        "New Enquiry Received",
                        "New enquiry from " + request.getContactPerson() + " regarding " + request.getProductName() + ".",
                        null
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send push notification for enquiry: " + e.getMessage());
        }

        return toResponse(saved);
    }

    public List<EnquiryResponse> getAllEnquiries(EnquiryType type) {
        List<EnquiryRequest> enquiries;
        if (type != null) {
            enquiries = enquiryRequestRepository.findAllByEnquiryTypeOrderByCreatedAtDesc(type);
        } else {
            enquiries = enquiryRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return enquiries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnquiryResponse updateEnquiryStatus(Long id, EnquiryStatus status) {
        EnquiryRequest enquiry = enquiryRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found with id: " + id));

        enquiry.setStatus(status);
        EnquiryRequest updated = enquiryRequestRepository.save(enquiry);
        return toResponse(updated);
    }
}
