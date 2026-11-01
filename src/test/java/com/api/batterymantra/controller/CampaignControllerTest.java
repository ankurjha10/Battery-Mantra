package com.api.batterymantra.controller;

import com.api.batterymantra.controller.admin.AdminCampaignController;
import com.api.batterymantra.dto.campaign.*;
import com.api.batterymantra.entity.enums.BadgeTheme;
import com.api.batterymantra.entity.enums.CampaignDiscountType;
import com.api.batterymantra.entity.enums.CampaignStatusFilter;
import com.api.batterymantra.entity.enums.CampaignTargetType;
import com.api.batterymantra.service.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    @InjectMocks
    private AdminCampaignController adminCampaignController;

    private ObjectMapper objectMapper;
    private CampaignResponse sampleCampaignResponse;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        publicMockMvc = MockMvcBuilders.standaloneSetup(campaignController).build();
        adminMockMvc = MockMvcBuilders.standaloneSetup(adminCampaignController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        campaignId = UUID.randomUUID();
        sampleCampaignResponse = CampaignResponse.builder()
                .id(campaignId)
                .name("Diwali Dhamaka Sale")
                .slug("diwali-dhamaka-sale")
                .tagline("Flat 500 off on exchange")
                .badgeText("Diwali Special")
                .badgeTheme(BadgeTheme.FESTIVE_GOLD)
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(5))
                .isActive(true)
                .priority(10)
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(500))
                .minOrderValue(BigDecimal.valueOf(2500))
                .couponCode("DIWALI500")
                .status("ACTIVE")
                .build();
    }

    // ==========================================
    // Public Storefront Endpoints
    // ==========================================

    @Test
    @DisplayName("GET /api/campaigns/active - Returns active campaign summaries")
    void testGetActiveCampaigns() throws Exception {
        when(campaignService.getActiveCampaigns()).thenReturn(List.of(sampleCampaignResponse));

        publicMockMvc.perform(get("/api/campaigns/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("diwali-dhamaka-sale"))
                .andExpect(jsonPath("$[0].name").value("Diwali Dhamaka Sale"))
                .andExpect(jsonPath("$[0].badgeTheme").value("FESTIVE_GOLD"))
                .andExpect(jsonPath("$[0].discountValue").value(500));
    }

    @Test
    @DisplayName("GET /api/campaigns/{slug} - Returns campaign and paginated products")
    void testGetCampaignBySlug() throws Exception {
        CampaignLandingResponse landingResponse = CampaignLandingResponse.builder()
                .campaign(sampleCampaignResponse)
                .products(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 24), 0))
                .build();

        when(campaignService.getCampaignBySlug(eq("diwali-dhamaka-sale"), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(landingResponse);

        publicMockMvc.perform(get("/api/campaigns/diwali-dhamaka-sale")
                        .param("page", "0")
                        .param("size", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.name").value("Diwali Dhamaka Sale"))
                .andExpect(jsonPath("$.campaign.slug").value("diwali-dhamaka-sale"))
                .andExpect(jsonPath("$.products.content").isArray());
    }

    @Test
    @DisplayName("POST /api/campaigns/validate-cart - Validates cart discount")
    void testValidateCart() throws Exception {
        ValidateCartResponse validateResponse = ValidateCartResponse.builder()
                .applicable(true)
                .campaignId(campaignId)
                .campaignName("Diwali Dhamaka Sale")
                .discountAmount(BigDecimal.valueOf(500))
                .finalPayable(BigDecimal.valueOf(3200))
                .message("Offer applied successfully!")
                .build();

        when(campaignService.validateCart(any(ValidateCartRequest.class))).thenReturn(validateResponse);

        ValidateCartRequest req = ValidateCartRequest.builder()
                .couponCode("DIWALI500")
                .items(List.of(CartValidateItemDto.builder()
                        .productId(UUID.randomUUID())
                        .quantity(1)
                        .hasExchange(true)
                        .build()))
                .build();

        publicMockMvc.perform(post("/api/campaigns/validate-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicable").value(true))
                .andExpect(jsonPath("$.campaignName").value("Diwali Dhamaka Sale"))
                .andExpect(jsonPath("$.discountAmount").value(500))
                .andExpect(jsonPath("$.finalPayable").value(3200))
                .andExpect(jsonPath("$.message").value("Offer applied successfully!"));
    }

    // ==========================================
    // Admin Endpoints
    // ==========================================

    @Test
    @DisplayName("GET /api/admin/campaigns - Paginated admin listing")
    void testAdminGetAllCampaigns() throws Exception {
        when(campaignService.getAllCampaigns(eq(CampaignStatusFilter.ALL), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleCampaignResponse), PageRequest.of(0, 20), 1));

        adminMockMvc.perform(get("/api/admin/campaigns")
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(campaignId.toString()));
    }

    @Test
    @DisplayName("POST /api/admin/campaigns - Create campaign returns 201 Created")
    void testAdminCreateCampaign() throws Exception {
        CampaignRequest req = CampaignRequest.builder()
                .name("Diwali Dhamaka Sale")
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(5))
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(500))
                .build();

        when(campaignService.createCampaign(any(CampaignRequest.class))).thenReturn(sampleCampaignResponse);

        adminMockMvc.perform(post("/api/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Diwali Dhamaka Sale"));
    }

    @Test
    @DisplayName("GET /api/admin/campaigns/{id} - Get single campaign")
    void testAdminGetCampaignById() throws Exception {
        when(campaignService.getCampaignById(campaignId)).thenReturn(sampleCampaignResponse);

        adminMockMvc.perform(get("/api/admin/campaigns/" + campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campaignId.toString()));
    }

    @Test
    @DisplayName("PUT /api/admin/campaigns/{id} - Update campaign")
    void testAdminUpdateCampaign() throws Exception {
        CampaignRequest req = CampaignRequest.builder()
                .name("Diwali Dhamaka Sale Updated")
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(5))
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .build();

        when(campaignService.updateCampaign(eq(campaignId), any(CampaignRequest.class))).thenReturn(sampleCampaignResponse);

        adminMockMvc.perform(put("/api/admin/campaigns/" + campaignId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campaignId.toString()));
    }

    @Test
    @DisplayName("DELETE /api/admin/campaigns/{id} - Delete campaign returns 204")
    void testAdminDeleteCampaign() throws Exception {
        doNothing().when(campaignService).deleteCampaign(campaignId);

        adminMockMvc.perform(delete("/api/admin/campaigns/" + campaignId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/admin/campaigns/{id}/toggle-status - Quick status toggle")
    void testAdminToggleStatus() throws Exception {
        sampleCampaignResponse.setIsActive(false);
        sampleCampaignResponse.setStatus("INACTIVE");
        when(campaignService.toggleCampaignStatus(campaignId)).thenReturn(sampleCampaignResponse);

        adminMockMvc.perform(patch("/api/admin/campaigns/" + campaignId + "/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
