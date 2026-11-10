package com.api.batterymantra.service;

import com.api.batterymantra.dto.user.CreateEngineerRequest;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.entity.EngineerProfile;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineerServiceTest {

    @Mock
    private EngineerProfileRepository engineerProfileRepository;

    @Mock
    private PartnerProfileRepository partnerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EngineerService engineerService;

    @Test
    @DisplayName("createPartnerEngineer: allows null/blank password by auto-generating and encoding random UUID")
    void testCreatePartnerEngineer_WithoutPassword_AutoGeneratesPassword() {
        UUID partnerId = UUID.randomUUID();
        PartnerProfile partner = PartnerProfile.builder()
                .id(partnerId)
                .businessName("Pune Battery Hub")
                .build();

        CreateEngineerRequest req = new CreateEngineerRequest();
        req.setFirstName("Ramesh");
        req.setLastName("Kumar");
        req.setEmail("ramesh@example.com");
        req.setPhoneNumber("9876543210");
        req.setPassword("   "); // blank password

        when(userRepository.existsByEmail("ramesh@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedRandomHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(partnerProfileRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(engineerProfileRepository.save(any(EngineerProfile.class))).thenAnswer(inv -> {
            EngineerProfile ep = inv.getArgument(0);
            ep.setId(UUID.randomUUID());
            return ep;
        });

        EngineerResponse res = engineerService.createPartnerEngineer(req, partnerId);

        assertNotNull(res);
        assertEquals("Ramesh", res.getFirstName());
        assertEquals(partnerId, res.getPartnerId());

        ArgumentCaptor<String> rawPwCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawPwCaptor.capture());
        assertDoesNotThrow(() -> UUID.fromString(rawPwCaptor.getValue()), "Generated password should be a valid UUID string");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.ENGINEER, userCaptor.getValue().getRole());
        assertEquals("$2a$10$encodedRandomHash", userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("createEngineer: uses explicit password when supplied")
    void testCreateEngineer_WithExplicitPassword_UsesSuppliedPassword() {
        CreateEngineerRequest req = new CreateEngineerRequest();
        req.setFirstName("Suresh");
        req.setLastName("Patil");
        req.setEmail("suresh@example.com");
        req.setPhoneNumber("9123456789");
        req.setPassword("SecretPass#1");

        when(userRepository.existsByEmail("suresh@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass#1")).thenReturn("$2a$10$encodedExplicit");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(engineerProfileRepository.save(any(EngineerProfile.class))).thenAnswer(inv -> {
            EngineerProfile ep = inv.getArgument(0);
            ep.setId(UUID.randomUUID());
            return ep;
        });

        EngineerResponse res = engineerService.createEngineer(req);

        assertNotNull(res);
        verify(passwordEncoder).encode("SecretPass#1");
    }
}

