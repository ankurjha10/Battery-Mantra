package com.api.batterymantra.controller;

import com.api.batterymantra.dto.inventory.CityInventoryBulkUpdateResponse;
import com.api.batterymantra.dto.inventory.CityProductStockItem;
import com.api.batterymantra.dto.inventory.PartnerBulkInventoryUpdateRequest;
import com.api.batterymantra.dto.inventory.PartnerInventoryItemResponse;
import com.api.batterymantra.dto.order.OrderNoteRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.dto.product.CityPricingRequestDto;
import com.api.batterymantra.dto.product.CityPricingRequestResponse;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PricingRequestStatus;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PartnerDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private EngineerService engineerService;

    @Mock
    private PartnerProfileRepository partnerProfileRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private CityPricingRequestService cityPricingRequestService;

    @Mock
    private CityInventoryService cityInventoryService;

    @InjectMocks
    private PartnerDashboardController controller;

    private ObjectMapper objectMapper;
    private UUID partnerProfileId;
    private UUID userId;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        partnerProfileId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User user = User.builder().userId(userId).username("partner_admin").build();
        testUserPrincipal = new UserPrincipal(user);

        PartnerProfile profile = PartnerProfile.builder().id(partnerProfileId).user(user).build();
        when(partnerProfileRepository.findByUserUserId(userId)).thenReturn(Optional.of(profile));

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return testUserPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("PUT /api/partner/orders/{orderId}/status updates status and passes cancellation reason")
    void testUpdateOrderStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setOrderStatus("CANCELLED");
        response.setCancellationReason("Customer rejected delivery");
        response.setCancelledBy("PARTNER");

        when(orderService.updatePartnerOrderStatus(eq(orderId), eq(OrderStatus.CANCELLED), eq("Customer rejected delivery"), eq(partnerProfileId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/partner/orders/{orderId}/status", orderId)
                        .param("newStatus", "CANCELLED")
                        .param("cancellationReason", "Customer rejected delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Customer rejected delivery"))
                .andExpect(jsonPath("$.cancelledBy").value("PARTNER"));
    }

    @Test
    @DisplayName("POST /api/partner/orders/{orderId}/notes adds internal notes")
    void testAddOrderNote() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderNoteRequest noteRequest = new OrderNoteRequest("Call customer 30 mins before arrival");

        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setEngineerNotes("Call customer 30 mins before arrival");

        when(orderService.addPartnerOrderNote(eq(orderId), eq("Call customer 30 mins before arrival"), eq(partnerProfileId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/partner/orders/{orderId}/notes", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engineerNotes").value("Call customer 30 mins before arrival"));
    }

    @Test
    @DisplayName("POST /api/partner/products/{productId}/city-pricing-request creates pending price request")
    void testCreateCityPricingRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();

        CityPricingRequestDto dto = CityPricingRequestDto.builder()
                .cityId(cityId)
                .price(BigDecimal.valueOf(4800.0))
                .exchangeDiscount(BigDecimal.valueOf(500.0))
                .stock(10)
                .reason("Local market supplier cost increased")
                .build();

        CityPricingRequestResponse response = CityPricingRequestResponse.builder()
                .requestId(UUID.randomUUID())
                .productId(productId)
                .cityId(cityId)
                .partnerId(partnerProfileId)
                .price(BigDecimal.valueOf(4800.0))
                .exchangeDiscount(BigDecimal.valueOf(500.0))
                .stock(10)
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .build();

        when(cityPricingRequestService.createPricingRequest(eq(productId), any(CityPricingRequestDto.class), any(PartnerProfile.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/partner/products/{productId}/city-pricing-request", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.price").value(4800.0))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    @DisplayName("GET /api/partner/products/pending-pricing-requests returns partner's pending requests")
    void testGetPendingPricingRequests() throws Exception {
        CityPricingRequestResponse responseItem = CityPricingRequestResponse.builder()
                .requestId(UUID.randomUUID())
                .partnerId(partnerProfileId)
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .price(BigDecimal.valueOf(4800.0))
                .build();

        when(cityPricingRequestService.getPendingRequestsForPartner(partnerProfileId))
                .thenReturn(List.of(responseItem));

        mockMvc.perform(get("/api/partner/products/pending-pricing-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$[0].price").value(4800.0));
    }

    @Test
    @DisplayName("GET /api/partner/inventory returns branch inventory for assigned city")
    void testGetBranchInventory() throws Exception {
        UUID cityId = UUID.randomUUID();
        PartnerInventoryItemResponse item = PartnerInventoryItemResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Exide Mileage ML38B20R")
                .productPrice(BigDecimal.valueOf(4299))
                .exchangeDiscount(BigDecimal.valueOf(600))
                .productStock(14)
                .brandName("Exide")
                .capacity("35Ah")
                .productCategory("Car Battery")
                .productImage("https://img.batterymantra.com/exide.png")
                .build();

        when(cityInventoryService.getPartnerCityInventory(eq(cityId), any(PartnerProfile.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/partner/inventory")
                        .param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Exide Mileage ML38B20R"))
                .andExpect(jsonPath("$[0].productPrice").value(4299))
                .andExpect(jsonPath("$[0].exchangeDiscount").value(600))
                .andExpect(jsonPath("$[0].productStock").value(14))
                .andExpect(jsonPath("$[0].brandName").value("Exide"));
    }

    @Test
    @DisplayName("PATCH /api/partner/inventory/{productId}/stock updates single product stock")
    void testUpdateBranchStock() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();

        PartnerInventoryItemResponse item = PartnerInventoryItemResponse.builder()
                .productId(productId)
                .productName("Exide Mileage ML38B20R")
                .productStock(20)
                .build();

        when(cityInventoryService.updatePartnerProductStock(eq(productId), eq(cityId), eq(20), any(PartnerProfile.class)))
                .thenReturn(item);

        mockMvc.perform(patch("/api/partner/inventory/{productId}/stock", productId)
                        .param("cityId", cityId.toString())
                        .param("stock", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.productStock").value(20));
    }

    @Test
    @DisplayName("POST /api/partner/inventory/bulk-update updates stock across multiple products")
    void testBulkUpdateBranchStock() throws Exception {
        UUID cityId = UUID.randomUUID();
        UUID prod1 = UUID.randomUUID();
        UUID prod2 = UUID.randomUUID();

        PartnerBulkInventoryUpdateRequest request = PartnerBulkInventoryUpdateRequest.builder()
                .cityId(cityId)
                .updates(List.of(
                        new CityProductStockItem(prod1, 10),
                        new CityProductStockItem(prod2, 0)
                ))
                .build();

        CityInventoryBulkUpdateResponse response = CityInventoryBulkUpdateResponse.builder()
                .cityId(cityId)
                .updatedCount(2)
                .message("Branch inventory updated successfully")
                .build();

        when(cityInventoryService.bulkUpdatePartnerCityStock(any(PartnerBulkInventoryUpdateRequest.class), any(PartnerProfile.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/partner/inventory/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId").value(cityId.toString()))
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.message").value("Branch inventory updated successfully"));
    }
}

