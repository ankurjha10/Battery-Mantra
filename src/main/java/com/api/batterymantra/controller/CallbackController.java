package com.api.batterymantra.controller;

import com.api.batterymantra.dto.callback.CallbackResponse;
import com.api.batterymantra.dto.callback.CreateCallbackRequest;
import com.api.batterymantra.service.CallbackRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/callbacks")
@RequiredArgsConstructor
public class CallbackController {

    private final CallbackRequestService callbackRequestService;

    @PostMapping
    public ResponseEntity<CallbackResponse> requestCallback(@RequestBody CreateCallbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(callbackRequestService.createCallbackRequest(request));
    }
}
