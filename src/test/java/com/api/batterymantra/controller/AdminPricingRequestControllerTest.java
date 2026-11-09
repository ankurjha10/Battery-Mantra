package com.api.batterymantra.controller;

import com.api.batterymantra.dto.product.CityPricingRequestResponse;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.entity.enums.PricingRequestStatus;
import com.api.batterymantra.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPricingRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminService adminService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private BrandService brandService;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private ProductService productService;
    @Mock
    private OrderService orderService;
    @Mock
    private BannerService bannerService;
    @Mock
    private CallbackRequestService callbackRequestService;
    @Mock
    private EnquiryService enquiryService;
    @Mock
    private EngineerAttendanceService attendanceService;
    @Mock
    private UserService userService;
    @Mock
    private InstagramReelService reelService;
    @Mock
    private CityPricingRequestService cityPricingRequestService;

    @InjectMocks
    private AdminController controller;

    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder().userId(UUID.randomUUID()).username("superadmin").build();
        adminPrincipal = new UserPrincipal(adminUser);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
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
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("PATCH /api/admin/products/pricing-requests/{requestId}/approve approves request")
    void testApprovePricingRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        CityPricingRequestResponse response = CityPricingRequestResponse.builder()
                .requestId(requestId)
                .status(PricingRequestStatus.APPROVED)
                .price(BigDecimal.valueOf(4800.0))
                .reviewedBy("superadmin")
                .build();

        when(cityPricingRequestService.approvePricingRequest(eq(requestId), eq("superadmin")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/products/pricing-requests/{requestId}/approve", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("superadmin"));
    }

    @Test
    @DisplayName("PATCH /api/admin/products/pricing-requests/{requestId}/reject rejects request")
    void testRejectPricingRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        CityPricingRequestResponse response = CityPricingRequestResponse.builder()
                .requestId(requestId)
                .status(PricingRequestStatus.REJECTED)
                .rejectionReason("Market price capped")
                .reviewedBy("superadmin")
                .build();

        when(cityPricingRequestService.rejectPricingRequest(eq(requestId), eq("Market price capped"), eq("superadmin")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/products/pricing-requests/{requestId}/reject", requestId)
                        .param("reason", "Market price capped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Market price capped"));
    }

    @Test
    @DisplayName("GET /api/admin/products/pricing-requests lists pricing requests")
    void testGetPricingRequests() throws Exception {
        CityPricingRequestResponse item = CityPricingRequestResponse.builder()
                .requestId(UUID.randomUUID())
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .build();

        when(cityPricingRequestService.getPricingRequests(PricingRequestStatus.PENDING_APPROVAL))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/products/pricing-requests")
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"));
    }
}
