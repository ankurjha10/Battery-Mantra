package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.CityPricingRequestDto;
import com.api.batterymantra.dto.product.CityPricingRequestResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.PricingRequestStatus;
import com.api.batterymantra.repository.CityPricingRequestRepository;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityPricingRequestServiceTest {

    @Mock
    private CityPricingRequestRepository cityPricingRequestRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityPricingRequestService service;

    private UUID productId;
    private UUID cityId;
    private UUID partnerId;
    private Product product;
    private City city;
    private PartnerProfile partnerProfile;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        cityId = UUID.randomUUID();
        partnerId = UUID.randomUUID();

        city = new City();
        city.setCityId(cityId);
        city.setCityName("Pune");

        product = new Product();
        product.setProductId(productId);
        product.setProductName("Exide Mileage ML38B20R");
        product.setProductPrice(BigDecimal.valueOf(4500.00));
        product.setCityPrices(new ArrayList<>());

        Set<City> operatingCities = new HashSet<>();
        operatingCities.add(city);

        partnerProfile = PartnerProfile.builder()
                .id(partnerId)
                .businessName("Pune Battery Centre")
                .operatingCities(operatingCities)
                .build();
    }

    @Test
    @DisplayName("Create pricing request creates PENDING_APPROVAL and does not modify live product price")
    void testCreatePricingRequest_Success() {
        CityPricingRequestDto dto = CityPricingRequestDto.builder()
                .cityId(cityId)
                .price(BigDecimal.valueOf(4800.00))
                .exchangeDiscount(BigDecimal.valueOf(500.00))
                .stock(10)
                .reason("Local supplier cost increase")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(cityPricingRequestRepository.save(any(CityPricingRequest.class))).thenAnswer(i -> {
            CityPricingRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CityPricingRequestResponse response = service.createPricingRequest(productId, dto, partnerProfile);

        assertNotNull(response);
        assertEquals(PricingRequestStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals(BigDecimal.valueOf(4800.00), response.getPrice());
        assertEquals("Pune", response.getCityName());
        assertEquals(productId, response.getProductId());

        // Verify live product prices list was NOT modified
        assertTrue(product.getCityPrices().isEmpty(), "Live product city prices should remain empty until admin approval");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Create pricing request rejects city not in partner's operating cities")
    void testCreatePricingRequest_RejectsUnauthorizedCity() {
        UUID otherCityId = UUID.randomUUID();
        City otherCity = new City();
        otherCity.setCityId(otherCityId);
        otherCity.setCityName("Mumbai");

        CityPricingRequestDto dto = CityPricingRequestDto.builder()
                .cityId(otherCityId)
                .price(BigDecimal.valueOf(5000.00))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cityRepository.findById(otherCityId)).thenReturn(Optional.of(otherCity));

        assertThrows(ResponseStatusException.class, () ->
                service.createPricingRequest(productId, dto, partnerProfile));
    }

    @Test
    @DisplayName("Approve pricing request applies price to live catalog and marks APPROVED")
    void testApprovePricingRequest_Success() {
        UUID requestId = UUID.randomUUID();
        CityPricingRequest request = CityPricingRequest.builder()
                .id(requestId)
                .product(product)
                .city(city)
                .partner(partnerProfile)
                .price(BigDecimal.valueOf(4900.00))
                .exchangeDiscount(BigDecimal.valueOf(550.00))
                .stock(15)
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .build();

        when(cityPricingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(cityPricingRequestRepository.save(any(CityPricingRequest.class))).thenAnswer(i -> i.getArgument(0));

        CityPricingRequestResponse response = service.approvePricingRequest(requestId, "admin_user");

        assertNotNull(response);
        assertEquals(PricingRequestStatus.APPROVED, response.getStatus());
        assertEquals("admin_user", response.getReviewedBy());
        assertNotNull(response.getReviewedAt());

        // Verify product was saved with new live city pricing
        verify(productRepository).save(product);
        assertEquals(1, product.getCityPrices().size());
        ProductCityPricing applied = product.getCityPrices().get(0);
        assertEquals(BigDecimal.valueOf(4900.00), applied.getPrice());
        assertEquals(BigDecimal.valueOf(550.00), applied.getExchangeDiscount());
        assertEquals(15, applied.getStock());
    }

    @Test
    @DisplayName("Reject pricing request leaves catalog untouched and marks REJECTED")
    void testRejectPricingRequest_Success() {
        UUID requestId = UUID.randomUUID();
        CityPricingRequest request = CityPricingRequest.builder()
                .id(requestId)
                .product(product)
                .city(city)
                .partner(partnerProfile)
                .price(BigDecimal.valueOf(4900.00))
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .build();

        when(cityPricingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(cityPricingRequestRepository.save(any(CityPricingRequest.class))).thenAnswer(i -> i.getArgument(0));

        CityPricingRequestResponse response = service.rejectPricingRequest(requestId, "Price too high for local market", "admin_user");

        assertNotNull(response);
        assertEquals(PricingRequestStatus.REJECTED, response.getStatus());
        assertEquals("Price too high for local market", response.getRejectionReason());
        assertEquals("admin_user", response.getReviewedBy());

        // Catalog untouched
        verify(productRepository, never()).save(any(Product.class));
        assertTrue(product.getCityPrices().isEmpty());
    }

    @Test
    @DisplayName("Cannot re-approve an already processed pricing request")
    void testAlreadyProcessedRequest_ThrowsException() {
        UUID requestId = UUID.randomUUID();
        CityPricingRequest request = CityPricingRequest.builder()
                .id(requestId)
                .status(PricingRequestStatus.APPROVED)
                .build();

        when(cityPricingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class, () ->
                service.approvePricingRequest(requestId, "admin"));
    }
}
