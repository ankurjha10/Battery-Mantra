package com.api.batterymantra.service;

import com.api.batterymantra.dto.callback.CallbackResponse;
import com.api.batterymantra.dto.callback.CreateCallbackRequest;
import com.api.batterymantra.entity.CallbackRequest;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.entity.enums.CallbackStatus;
import com.api.batterymantra.repository.CallbackRequestRepository;
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
public class CallbackRequestService {

    private final CallbackRequestRepository callbackRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private CallbackResponse toResponse(CallbackRequest request) {
        CallbackResponse response = new CallbackResponse();
        response.setCallbackId(request.getCallbackId());
        response.setMobileNumber(request.getMobileNumber());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        return response;
    }

    @Transactional
    public CallbackResponse createCallbackRequest(CreateCallbackRequest request) {
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }

        CallbackRequest callback = new CallbackRequest();
        callback.setMobileNumber(request.getMobileNumber());
        callback.setStatus(CallbackStatus.PENDING);
        
        CallbackRequest saved = callbackRequestRepository.save(callback);

        try {
            List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
            for (User admin : admins) {
                notificationService.sendPushNotification(
                        admin.getUserId(),
                        "New Callback Request",
                        "A new callback request has been received from " + request.getMobileNumber() + ".",
                        null
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send push notification for callback: " + e.getMessage());
        }

        return toResponse(saved);
    }

    public List<CallbackResponse> getAllCallbackRequests() {
        return callbackRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CallbackResponse updateCallbackStatus(Long id, CallbackStatus status) {
        CallbackRequest callback = callbackRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Callback request not found with id: " + id));

        callback.setStatus(status);
        CallbackRequest updated = callbackRequestRepository.save(callback);
        return toResponse(updated);
    }
}
