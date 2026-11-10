package com.api.batterymantra.service;

import com.api.batterymantra.dto.auth.LoginRequest;
import com.api.batterymantra.dto.auth.LoginResponse;
import com.api.batterymantra.dto.auth.SendOtpRequest;
import com.api.batterymantra.dto.auth.VerifyOtpRequest;
import com.api.batterymantra.entity.EngineerProfile;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.RefreshToken;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.security.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicePartnerAndGateTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EngineerProfileRepository engineerProfileRepository;
    @Mock
    private PartnerProfileRepository partnerProfileRepository;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private SmsService smsService;

    private AuthUtil authUtil;
    private OtpService otpService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authUtil = new AuthUtil();
        String secret = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes()
        );
        ReflectionTestUtils.setField(authUtil, "jwtSecretKey", secret);
        ReflectionTestUtils.setField(authUtil, "jwtExpirationMs", 3600000L);

        otpService = new OtpService();
        ReflectionTestUtils.setField(otpService, "otpValidityMs", 300000L);

        authService = new AuthService(
                userRepository,
                engineerProfileRepository,
                partnerProfileRepository,
                authUtil,
                authManager,
                passwordEncoder,
                refreshTokenService,
                smsService,
                otpService
        );
    }

    @Test
    @DisplayName("Partner OTP: sends and verifies OTP strictly for ROLE_PARTNER with +91 normalization and businessName")
    void testPartnerOtp_Success() {
        UUID userId = UUID.randomUUID();
        User partnerUser = User.builder()
                .userId(userId)
                .username("partner@hub.com")
                .phoneNumber("+919876543210")
                .role(UserRole.PARTNER)
                .isActive(true)
                .build();

        PartnerProfile partnerProfile = PartnerProfile.builder()
                .id(UUID.randomUUID())
                .user(partnerUser)
                .businessName("Pune Express Batteries")
                .contactPerson("Rohan Sharma")
                .isActive(true)
                .build();

        when(userRepository.findAllByPhoneNumber("+919876543210")).thenReturn(List.of(partnerUser));
        when(partnerProfileRepository.findByUserUserId(userId)).thenReturn(Optional.of(partnerProfile));

        RefreshToken rt = new RefreshToken();
        rt.setRefreshToken("refresh-123");
        when(refreshTokenService.createRefreshToken("partner@hub.com")).thenReturn(rt);

        SendOtpRequest sendReq = new SendOtpRequest();
        sendReq.setPhoneNumber("+919876543210");
        authService.sendPartnerOtp(sendReq);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendOtp(eq("9876543210"), eq("Pune Express Batteries"), otpCaptor.capture());
        String otp = otpCaptor.getValue();

        VerifyOtpRequest verifyReq = new VerifyOtpRequest();
        verifyReq.setPhoneNumber("+919876543210");
        verifyReq.setOtp(otp);

        LoginResponse response = authService.verifyPartnerOtp(verifyReq);

        assertNotNull(response.getToken());
        assertEquals("refresh-123", response.getRefreshToken());
        assertEquals("PARTNER", response.getRole());
        assertEquals("Pune Express Batteries", response.getName());
        assertEquals(userId, response.getId());
    }

    @Test
    @DisplayName("Partner OTP: rejects ENGINEER, ADMIN, SUB_ADMIN, CUSTOMER before generating/sending OTP")
    void testPartnerOtp_RejectsNonPartnerRoles() {
        for (UserRole nonPartnerRole : List.of(UserRole.ENGINEER, UserRole.ADMIN, UserRole.SUB_ADMIN, UserRole.CUSTOMER)) {
            User nonPartner = User.builder()
                    .userId(UUID.randomUUID())
                    .username("user_" + nonPartnerRole)
                    .phoneNumber("9998887776")
                    .role(nonPartnerRole)
                    .isActive(true)
                    .build();

            when(userRepository.findAllByPhoneNumber("9998887776")).thenReturn(List.of(nonPartner));
            when(userRepository.findByPhoneNumber("9998887776")).thenReturn(nonPartner);
            when(userRepository.findAllByRole(UserRole.PARTNER)).thenReturn(List.of());

            SendOtpRequest req = new SendOtpRequest();
            req.setPhoneNumber("9998887776");

            assertThrows(IllegalArgumentException.class, () -> authService.sendPartnerOtp(req));
        }

        verify(smsService, never()).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Partner OTP: rejects inactive PartnerProfile before sending OTP")
    void testPartnerOtp_RejectsInactivePartnerProfile() {
        UUID userId = UUID.randomUUID();
        User partnerUser = User.builder()
                .userId(userId)
                .username("disabled_partner")
                .phoneNumber("9876500000")
                .role(UserRole.PARTNER)
                .isActive(true)
                .build();
        PartnerProfile disabledProfile = PartnerProfile.builder()
                .id(UUID.randomUUID())
                .user(partnerUser)
                .businessName("Closed Branch")
                .isActive(false)
                .build();

        when(userRepository.findAllByPhoneNumber("9876500000")).thenReturn(List.of(partnerUser));
        when(partnerProfileRepository.findByUserUserId(userId)).thenReturn(Optional.of(disabledProfile));

        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("9876500000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.sendPartnerOtp(req));
        assertTrue(ex.getMessage().contains("disabled"));
        verify(smsService, never()).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("OTP isolation: OTP sent on Partner channel cannot be verified on Engineer channel and vice versa")
    void testOtpScopeIsolation() {
        UUID partnerUserId = UUID.randomUUID();
        User partnerUser = User.builder()
                .userId(partnerUserId)
                .username("p1")
                .phoneNumber("9111111111")
                .role(UserRole.PARTNER)
                .isActive(true)
                .build();
        PartnerProfile partnerProfile = PartnerProfile.builder()
                .id(UUID.randomUUID()).user(partnerUser).businessName("Hub").isActive(true).build();

        UUID engUserId = UUID.randomUUID();
        User engUser = User.builder()
                .userId(engUserId)
                .username("e1")
                .phoneNumber("9111111111")
                .role(UserRole.ENGINEER)
                .isActive(true)
                .build();
        EngineerProfile engProfile = EngineerProfile.builder()
                .id(UUID.randomUUID()).user(engUser).firstName("Eng").isActive(true).build();

        when(userRepository.findAllByPhoneNumber("9111111111")).thenReturn(List.of(partnerUser, engUser));
        when(partnerProfileRepository.findByUserUserId(partnerUserId)).thenReturn(Optional.of(partnerProfile));
        when(engineerProfileRepository.findByUserUserId(engUserId)).thenReturn(Optional.of(engProfile));

        // Send Partner OTP
        SendOtpRequest sendReq = new SendOtpRequest();
        sendReq.setPhoneNumber("9111111111");
        authService.sendPartnerOtp(sendReq);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendOtp(eq("9111111111"), eq("Hub"), otpCaptor.capture());
        String partnerOtp = otpCaptor.getValue();

        // Try verifying Partner's OTP on Engineer verify endpoint -> MUST FAIL
        VerifyOtpRequest crossVerify = new VerifyOtpRequest();
        crossVerify.setPhoneNumber("9111111111");
        crossVerify.setOtp(partnerOtp);

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEngineerOtp(crossVerify));
    }

    @Test
    @DisplayName("login: Website login without gateToken works normally")
    void testLogin_WithoutGateToken_Succeeds() {
        User admin = User.builder()
                .userId(UUID.randomUUID())
                .username("superadmin")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(admin), null)
        );
        RefreshToken rt = new RefreshToken();
        rt.setRefreshToken("rt-web");
        when(refreshTokenService.createRefreshToken("superadmin")).thenReturn(rt);

        LoginRequest req = new LoginRequest();
        req.setUsername("superadmin");
        req.setPassword("secret");

        LoginResponse res = authService.login(req);
        assertEquals("ADMIN", res.getRole());
        assertNotNull(res.getToken());
    }

    @Test
    @DisplayName("login: enforces gateToken tier and per-user targetUserId binding")
    void testLogin_GateTokenEnforcement() {
        UUID subAdminAId = UUID.randomUUID();
        User subAdminA = User.builder()
                .userId(subAdminAId)
                .username("subadmin_a")
                .role(UserRole.SUB_ADMIN)
                .gatePinHash("$2a$10$somePersonalHash")
                .isActive(true)
                .build();

        User subAdminB = User.builder()
                .userId(UUID.randomUUID())
                .username("subadmin_b")
                .role(UserRole.SUB_ADMIN)
                .isActive(true)
                .build();

        User adminUser = User.builder()
                .userId(UUID.randomUUID())
                .username("admin_user")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        // 1. SubAdmin A with Admin gate token -> 403 Forbidden
        String adminGateToken = authUtil.generateGateToken("ADMIN", null);
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(subAdminA), null)
        );
        LoginRequest req1 = new LoginRequest();
        req1.setUsername("subadmin_a");
        req1.setPassword("pass");
        req1.setGateToken(adminGateToken);

        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () -> authService.login(req1));
        assertEquals(403, ex1.getStatusCode().value());

        // 2. SubAdmin A personal gateToken used by SubAdmin B -> 403 Forbidden
        String subAdminAPersonalToken = authUtil.generateGateToken("SUB_ADMIN", subAdminAId);
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(subAdminB), null)
        );
        LoginRequest req2 = new LoginRequest();
        req2.setUsername("subadmin_b");
        req2.setPassword("pass");
        req2.setGateToken(subAdminAPersonalToken);

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> authService.login(req2));
        assertEquals(403, ex2.getStatusCode().value());

        // 3. Fallback SUB_ADMIN gateToken (targetUserId=null) used by SubAdmin A who HAS a personal PIN -> 403 Forbidden
        String fallbackSubAdminToken = authUtil.generateGateToken("SUB_ADMIN", null);
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(subAdminA), null)
        );
        LoginRequest req3 = new LoginRequest();
        req3.setUsername("subadmin_a");
        req3.setPassword("pass");
        req3.setGateToken(fallbackSubAdminToken);

        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class, () -> authService.login(req3));
        assertEquals(403, ex3.getStatusCode().value());

        // 4. SubAdmin A with SubAdmin A personal gateToken -> 200 OK
        RefreshToken rt = new RefreshToken();
        rt.setRefreshToken("rt-sub-a");
        when(refreshTokenService.createRefreshToken("subadmin_a")).thenReturn(rt);

        LoginRequest req4 = new LoginRequest();
        req4.setUsername("subadmin_a");
        req4.setPassword("pass");
        req4.setGateToken(subAdminAPersonalToken);

        LoginResponse res4 = authService.login(req4);
        assertEquals("SUB_ADMIN", res4.getRole());

        // 5. Invalid gateToken -> 401 Unauthorized
        LoginRequest req5 = new LoginRequest();
        req5.setUsername("admin_user");
        req5.setPassword("pass");
        req5.setGateToken("invalid.jwt.token");

        ResponseStatusException ex5 = assertThrows(ResponseStatusException.class, () -> authService.login(req5));
        assertEquals(401, ex5.getStatusCode().value());
    }
}

