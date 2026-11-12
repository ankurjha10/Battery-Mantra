package com.api.batterymantra.controller.admin;

import com.api.batterymantra.dto.auth.AdminGateConfigResponse;
import com.api.batterymantra.dto.auth.UpdateGatePinRequest;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.exception.GlobalExceptionHandler;
import com.api.batterymantra.service.AdminGateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminSecurityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminGateService adminGateService;

    @InjectMocks
    private AdminSecurityController controller;

    private ObjectMapper objectMapper;
    private UUID adminUserId;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        adminUserId = UUID.randomUUID();
        User admin = User.builder().userId(adminUserId).username("superadmin").role(UserRole.ADMIN).build();
        adminPrincipal = new UserPrincipal(admin);

        HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return adminPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/security/gate-config returns configured flags and user list without raw PINs")
    void testGetGateConfig() throws Exception {
        AdminGateConfigResponse response = AdminGateConfigResponse.builder()
                .adminPinConfigured(true)
                .subAdminPinConfigured(true)
                .updatedAt(LocalDateTime.now())
                .users(List.of(
                        AdminGateConfigResponse.UserGatePinStatus.builder()
                                .userId(UUID.randomUUID())
                                .username("subadmin_1")
                                .role("SUB_ADMIN")
                                .isActive(true)
                                .personalPinConfigured(true)
                                .build()
                ))
                .build();

        when(adminGateService.getGateConfig()).thenReturn(response);

        mockMvc.perform(get("/api/admin/security/gate-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminPinConfigured").value(true))
                .andExpect(jsonPath("$.subAdminPinConfigured").value(true))
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].personalPinConfigured").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/security/gate-pin updates tier or user gate PIN")
    void testUpdateGatePin() throws Exception {
        UpdateGatePinRequest req = UpdateGatePinRequest.builder()
                .targetRole("SUB_ADMIN")
                .newPin("654321")
                .currentPassword("SuperSecret@1")
                .build();

        AdminGateConfigResponse response = AdminGateConfigResponse.builder()
                .adminPinConfigured(true)
                .subAdminPinConfigured(true)
                .updatedAt(LocalDateTime.now())
                .users(List.of())
                .build();

        when(adminGateService.updateGatePin(eq(adminUserId), any(UpdateGatePinRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/security/gate-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminPinConfigured").value(true))
                .andExpect(jsonPath("$.subAdminPinConfigured").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/security/users/{userId}/gate-pin updates specific user's personal gate PIN")
    void testUpdateUserGatePin() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UpdateGatePinRequest req = UpdateGatePinRequest.builder()
                .newPin("776655")
                .currentPassword("SuperSecret@1")
                .build();

        AdminGateConfigResponse response = AdminGateConfigResponse.builder()
                .adminPinConfigured(true)
                .subAdminPinConfigured(true)
                .updatedAt(LocalDateTime.now())
                .users(List.of(
                        AdminGateConfigResponse.UserGatePinStatus.builder()
                                .userId(targetUserId)
                                .personalPinConfigured(true)
                                .build()
                ))
                .build();

        when(adminGateService.updateGatePin(eq(adminUserId), any(UpdateGatePinRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/security/users/{userId}/gate-pin", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.users[0].personalPinConfigured").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/security/gate-pin accepts raw tier-level JSON without targetUserId (4-digit ADMIN PIN)")
    void testUpdateGatePin_RawTierPayloadWithoutTargetUserId() throws Exception {
        AdminGateConfigResponse response = AdminGateConfigResponse.builder()
                .adminPinConfigured(true)
                .subAdminPinConfigured(false)
                .updatedAt(LocalDateTime.now())
                .users(List.of())
                .build();

        when(adminGateService.updateGatePin(eq(adminUserId), any(UpdateGatePinRequest.class)))
                .thenReturn(response);

        String rawPayload = """
                {
                  "targetRole": "ADMIN",
                  "newPin": "2244",
                  "currentPassword": "SuperSecret@1"
                }
                """;

        mockMvc.perform(put("/api/admin/security/gate-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminPinConfigured").value(true))
                .andExpect(jsonPath("$.subAdminPinConfigured").value(false));
    }
}

