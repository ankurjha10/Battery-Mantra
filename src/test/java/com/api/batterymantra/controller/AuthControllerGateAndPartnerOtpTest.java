package com.api.batterymantra.controller;

import com.api.batterymantra.dto.auth.*;
import com.api.batterymantra.exception.GlobalExceptionHandler;
import com.api.batterymantra.service.AdminGateService;
import com.api.batterymantra.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerGateAndPartnerOtpTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private AdminGateService adminGateService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/partner/send-otp returns 200 OK for valid partner phone")
    void testPartnerSendOtp_Success() throws Exception {
        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("9876543210");

        mockMvc.perform(post("/api/auth/partner/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent successfully"));
    }

    @Test
    @DisplayName("POST /api/auth/partner/send-otp returns 400 Bad Request for non-partner role")
    void testPartnerSendOtp_Rejected() throws Exception {
        doThrow(new IllegalArgumentException("No partner account found with this phone number."))
                .when(authService).sendPartnerOtp(any(SendOtpRequest.class));

        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("9999999999");

        mockMvc.perform(post("/api/auth/partner/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No partner account found with this phone number."));
    }

    @Test
    @DisplayName("POST /api/auth/partner/verify-otp returns LoginResponse with PARTNER role")
    void testPartnerVerifyOtp_Success() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setPhoneNumber("9876543210");
        req.setOtp("1234");

        LoginResponse res = new LoginResponse(
                "jwt-token", "refresh-token", UUID.randomUUID(), "PARTNER", "Pune Battery Hub", List.of()
        );
        when(authService.verifyPartnerOtp(any(VerifyOtpRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/partner/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("PARTNER"))
                .andExpect(jsonPath("$.name").value("Pune Battery Hub"));
    }

    @Test
    @DisplayName("POST /api/auth/admin-gate returns tier and gateToken on valid PIN")
    void testAdminGate_Success() throws Exception {
        AdminGateRequest req = new AdminGateRequest("849201");
        AdminGateResponse res = new AdminGateResponse("ADMIN", "signed.gate.token");

        when(adminGateService.verifyGatePin(any(AdminGateRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/admin-gate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("ADMIN"))
                .andExpect(jsonPath("$.gateToken").value("signed.gate.token"));
    }

    @Test
    @DisplayName("POST /api/auth/admin-gate returns 401 Unauthorized on invalid PIN")
    void testAdminGate_Unauthorized() throws Exception {
        when(adminGateService.verifyGatePin(any(AdminGateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid gate PIN"));

        AdminGateRequest req = new AdminGateRequest("000000");

        mockMvc.perform(post("/api/auth/admin-gate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid gate PIN"));
    }
}

