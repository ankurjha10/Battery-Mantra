package com.api.batterymantra.service;

import com.api.batterymantra.dto.auth.AdminGateConfigResponse;
import com.api.batterymantra.dto.auth.AdminGateRequest;
import com.api.batterymantra.dto.auth.AdminGateResponse;
import com.api.batterymantra.dto.auth.UpdateGatePinRequest;
import com.api.batterymantra.entity.AdminGateConfig;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.AdminGateConfigRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.security.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGateServiceTest {

    @Mock
    private AdminGateConfigRepository adminGateConfigRepository;

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthUtil authUtil;
    private AdminGateService adminGateService;

    private AdminGateConfig storedConfig;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authUtil = new AuthUtil();
        String secret = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes()
        );
        ReflectionTestUtils.setField(authUtil, "jwtSecretKey", secret);
        ReflectionTestUtils.setField(authUtil, "jwtExpirationMs", 3600000L);

        adminGateService = new AdminGateService(
                adminGateConfigRepository,
                userRepository,
                passwordEncoder,
                authUtil
        );

        storedConfig = AdminGateConfig.builder()
                .id(AdminGateConfig.SINGLETON_ID)
                .adminPinHash(passwordEncoder.encode("849201"))
                .subAdminPinHash(passwordEncoder.encode("517309"))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("verifyGatePin: matches per-user personal PIN first and binds gateToken to targetUserId")
    void testVerifyGatePin_PersonalPinMatchesAndBindsUserId() {
        UUID subAdminId = UUID.randomUUID();
        User subAdmin = User.builder()
                .userId(subAdminId)
                .username("subadmin_rakesh")
                .role(UserRole.SUB_ADMIN)
                .isActive(true)
                .gatePinHash(passwordEncoder.encode("224466"))
                .build();

        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.findAllByRole(UserRole.SUB_ADMIN)).thenReturn(List.of(subAdmin));

        AdminGateResponse res = adminGateService.verifyGatePin(new AdminGateRequest("224466"));

        assertEquals("SUB_ADMIN", res.getTier());
        AuthUtil.GateTokenClaims claims = authUtil.validateAndExtractGateClaims(res.getGateToken());
        assertEquals("SUB_ADMIN", claims.tier());
        assertEquals(subAdminId, claims.targetUserId());
    }

    @Test
    @DisplayName("verifyGatePin: falls back to tier PIN when no personal PIN matches, and throws 401 on wrong PIN")
    void testVerifyGatePin_FallbackAndMismatch() {
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.findAllByRole(UserRole.SUB_ADMIN)).thenReturn(List.of());
        when(adminGateConfigRepository.findById(AdminGateConfig.SINGLETON_ID)).thenReturn(Optional.of(storedConfig));

        AdminGateResponse adminRes = adminGateService.verifyGatePin(new AdminGateRequest("849201"));
        assertEquals("ADMIN", adminRes.getTier());
        assertNull(authUtil.validateAndExtractGateClaims(adminRes.getGateToken()).targetUserId());

        AdminGateResponse subAdminRes = adminGateService.verifyGatePin(new AdminGateRequest("517309"));
        assertEquals("SUB_ADMIN", subAdminRes.getTier());
        assertNull(authUtil.validateAndExtractGateClaims(subAdminRes.getGateToken()).targetUserId());

        ResponseStatusException badEx = assertThrows(ResponseStatusException.class, () ->
                adminGateService.verifyGatePin(new AdminGateRequest("000000")));
        assertEquals(401, badEx.getStatusCode().value());
    }

    @Test
    @DisplayName("updateGatePin: Super Admin can assign individual Sub-Admin PIN and enforces uniqueness")
    void testUpdateGatePin_PersonalPinAndUniqueness() {
        UUID superAdminId = UUID.randomUUID();
        User superAdmin = User.builder()
                .userId(superAdminId)
                .username("superadmin")
                .role(UserRole.ADMIN)
                .password(passwordEncoder.encode("AdminPass@123"))
                .isActive(true)
                .build();

        UUID subAdmin1Id = UUID.randomUUID();
        User subAdmin1 = User.builder()
                .userId(subAdmin1Id)
                .username("sub1")
                .role(UserRole.SUB_ADMIN)
                .isActive(true)
                .build();

        UUID subAdmin2Id = UUID.randomUUID();
        User subAdmin2 = User.builder()
                .userId(subAdmin2Id)
                .username("sub2")
                .role(UserRole.SUB_ADMIN)
                .isActive(true)
                .gatePinHash(passwordEncoder.encode("778899"))
                .build();

        when(userRepository.findById(superAdminId)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(subAdmin1Id)).thenReturn(Optional.of(subAdmin1));
        when(adminGateConfigRepository.findById(AdminGateConfig.SINGLETON_ID)).thenReturn(Optional.of(storedConfig));
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(superAdmin));
        when(userRepository.findAllByRole(UserRole.SUB_ADMIN)).thenReturn(List.of(subAdmin1, subAdmin2));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Collision with subAdmin2's PIN (778899) -> 400 Bad Request
        UpdateGatePinRequest collisionReq = UpdateGatePinRequest.builder()
                .targetUserId(subAdmin1Id)
                .newPin("778899")
                .currentPassword("AdminPass@123")
                .build();
        ResponseStatusException colEx = assertThrows(ResponseStatusException.class, () ->
                adminGateService.updateGatePin(superAdminId, collisionReq));
        assertEquals(400, colEx.getStatusCode().value());

        // 2. Collision with fallback ADMIN PIN (849201) -> 400 Bad Request
        UpdateGatePinRequest fallbackColReq = UpdateGatePinRequest.builder()
                .targetUserId(subAdmin1Id)
                .newPin("849201")
                .currentPassword("AdminPass@123")
                .build();
        assertThrows(ResponseStatusException.class, () ->
                adminGateService.updateGatePin(superAdminId, fallbackColReq));

        // 3. Unique PIN (334455) -> succeeds
        UpdateGatePinRequest validReq = UpdateGatePinRequest.builder()
                .targetUserId(subAdmin1Id)
                .newPin("334455")
                .currentPassword("AdminPass@123")
                .build();
        AdminGateConfigResponse cfgRes = adminGateService.updateGatePin(superAdminId, validReq);
        assertTrue(cfgRes.isAdminPinConfigured());
        assertTrue(cfgRes.isSubAdminPinConfigured());
        assertTrue(passwordEncoder.matches("334455", subAdmin1.getGatePinHash()));
        assertNotNull(subAdmin1.getGatePinUpdatedAt());
    }

    @Test
    @DisplayName("updateGatePin: rejects SUB_ADMIN caller with 403 and wrong password with 400")
    void testUpdateGatePin_AuthorizationAndPasswordChecks() {
        UUID subAdminCallerId = UUID.randomUUID();
        User subAdminCaller = User.builder()
                .userId(subAdminCallerId)
                .role(UserRole.SUB_ADMIN)
                .password(passwordEncoder.encode("pass"))
                .build();

        when(userRepository.findById(subAdminCallerId)).thenReturn(Optional.of(subAdminCaller));

        UpdateGatePinRequest req = UpdateGatePinRequest.builder()
                .targetRole("ADMIN")
                .newPin("998877")
                .currentPassword("pass")
                .build();

        ResponseStatusException forbiddenEx = assertThrows(ResponseStatusException.class, () ->
                adminGateService.updateGatePin(subAdminCallerId, req));
        assertEquals(403, forbiddenEx.getStatusCode().value());

        UUID superAdminId = UUID.randomUUID();
        User superAdmin = User.builder()
                .userId(superAdminId)
                .role(UserRole.ADMIN)
                .password(passwordEncoder.encode("realPass"))
                .build();
        when(userRepository.findById(superAdminId)).thenReturn(Optional.of(superAdmin));

        UpdateGatePinRequest wrongPwReq = UpdateGatePinRequest.builder()
                .targetRole("ADMIN")
                .newPin("998877")
                .currentPassword("wrongPass")
                .build();
        ResponseStatusException badPwEx = assertThrows(ResponseStatusException.class, () ->
                adminGateService.updateGatePin(superAdminId, wrongPwReq));
        assertEquals(400, badPwEx.getStatusCode().value());
    }

    @Test
    @DisplayName("getGateConfig & verifyGatePin: purely DB-driven, returns unconfigured flags until saved in DB")
    void testGetGateConfig_PureDbDriven() {
        when(adminGateConfigRepository.findById(AdminGateConfig.SINGLETON_ID)).thenReturn(Optional.empty());
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.findAllByRole(UserRole.SUB_ADMIN)).thenReturn(List.of());

        AdminGateConfigResponse response = adminGateService.getGateConfig();
        assertFalse(response.isAdminPinConfigured());
        assertFalse(response.isSubAdminPinConfigured());
        assertNull(response.getUpdatedAt());

        ResponseStatusException unconfiguredEx = assertThrows(ResponseStatusException.class, () ->
                adminGateService.verifyGatePin(new AdminGateRequest("849201")));
        assertEquals(401, unconfiguredEx.getStatusCode().value());

        // Now save tier PIN into DB via Super Admin
        UUID superAdminId = UUID.randomUUID();
        User superAdmin = User.builder()
                .userId(superAdminId)
                .username("superadmin")
                .role(UserRole.ADMIN)
                .password(passwordEncoder.encode("realPass"))
                .build();
        when(userRepository.findById(superAdminId)).thenReturn(Optional.of(superAdmin));
        when(adminGateConfigRepository.save(any(AdminGateConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminGateConfigResponse updated = adminGateService.updateGatePin(superAdminId,
                UpdateGatePinRequest.builder()
                        .targetRole("ADMIN")
                        .newPin("774411")
                        .currentPassword("realPass")
                        .build());

        assertTrue(updated.isAdminPinConfigured());
        assertFalse(updated.isSubAdminPinConfigured());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @DisplayName("updateGatePin: setting ADMIN tier PIN when sub_admin_pin_hash is null or blank succeeds and stays distinct from SUB_ADMIN")
    void testUpdateGatePin_TierAdminFirstThenSubAdmin() {
        UUID superAdminId = UUID.randomUUID();
        User superAdmin = User.builder()
                .userId(superAdminId)
                .username("superadmin")
                .role(UserRole.ADMIN)
                .password(passwordEncoder.encode("realPass"))
                .build();

        AdminGateConfig configState = AdminGateConfig.builder()
                .id(AdminGateConfig.SINGLETON_ID)
                .adminPinHash("")
                .subAdminPinHash("")
                .build();

        when(userRepository.findById(superAdminId)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(superAdmin));
        when(userRepository.findAllByRole(UserRole.SUB_ADMIN)).thenReturn(List.of());
        when(adminGateConfigRepository.findById(AdminGateConfig.SINGLETON_ID)).thenReturn(Optional.of(configState));
        when(adminGateConfigRepository.save(any(AdminGateConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminGateConfigResponse afterAdmin = adminGateService.updateGatePin(superAdminId,
                UpdateGatePinRequest.builder()
                        .targetRole("ADMIN")
                        .newPin("2244")
                        .currentPassword("realPass")
                        .build());

        assertTrue(afterAdmin.isAdminPinConfigured());
        assertFalse(afterAdmin.isSubAdminPinConfigured());
        assertTrue(passwordEncoder.matches("2244", configState.getAdminPinHash()));

        AdminGateConfigResponse afterSubAdmin = adminGateService.updateGatePin(superAdminId,
                UpdateGatePinRequest.builder()
                        .targetRole("SUB_ADMIN")
                        .newPin("3355")
                        .currentPassword("realPass")
                        .build());

        assertTrue(afterSubAdmin.isAdminPinConfigured());
        assertTrue(afterSubAdmin.isSubAdminPinConfigured());
        assertTrue(passwordEncoder.matches("3355", configState.getSubAdminPinHash()));
    }
}

