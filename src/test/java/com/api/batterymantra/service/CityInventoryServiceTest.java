package com.api.batterymantra.service;

import com.api.batterymantra.dto.inventory.*;
import com.api.batterymantra.entity.*;
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
class CityInventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityInventoryService cityInventoryService;

    private UUID puneCityId;
    private UUID mumbaiCityId;
    private UUID productId;
    private City puneCity;
    private City mumbaiCity;
    private PartnerProfile partnerProfile;
    private Product product;

    @BeforeEach
    void setUp() {
        puneCityId = UUID.randomUUID();
        mumbaiCityId = UUID.randomUUID();
        productId = UUID.randomUUID();

        puneCity = new City();
        puneCity.setCityId(puneCityId);
        puneCity.setCityName("Pune");

        mumbaiCity = new City();
        mumbaiCity.setCityId(mumbaiCityId);
        mumbaiCity.setCityName("Mumbai");

        // Partner has access to Pune ONLY
        partnerProfile = PartnerProfile.builder()
                .id(UUID.randomUUID())
                .businessName("Pune Battery World")
                .operatingCities(new HashSet<>(Collections.singletonList(puneCity)))
                .build();

        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setCategoryName("Car Battery");

        Brand brand = new Brand();
        brand.setBrandId(UUID.randomUUID());
        brand.setBrandName("Exide");

        product = new Product();
        product.setProductId(productId);
        product.setProductName("Exide Mileage ML38B20R");
        product.setProductPrice(BigDecimal.valueOf(4500));
        product.setExchangeDiscount(BigDecimal.valueOf(500));
        product.setProductStock(100); // Central warehouse stock
        product.setProductCategory(category);
        product.setBrand(brand);
        product.setCapacity("35Ah");
        product.setProductImage("https://img.batterymantra.com/exide.png");
        product.setApproved(true);
        product.setCityPrices(new ArrayList<>());
    }

    @Test
    @DisplayName("Partner accessing unassigned city is blocked with HTTP 403 Forbidden")
    void testPartnerCityIsolation_UnassignedCity_Forbidden() {
        // Partner tries to access Mumbai, but only assigned Pune
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                cityInventoryService.getPartnerCityInventory(mumbaiCityId, partnerProfile));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Access denied: You are not authorized to manage inventory for this city"));
    }

    @Test
    @DisplayName("Partner updating unassigned city stock is blocked with HTTP 403 Forbidden")
    void testPartnerUpdate_UnassignedCity_Forbidden() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                cityInventoryService.updatePartnerProductStock(productId, mumbaiCityId, 15, partnerProfile));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Access denied: You are not authorized to manage inventory for this city"));
    }

    @Test
    @DisplayName("Partner bulk updating unassigned city stock is blocked with HTTP 403 Forbidden")
    void testPartnerBulkUpdate_UnassignedCity_Forbidden() {
        PartnerBulkInventoryUpdateRequest request = PartnerBulkInventoryUpdateRequest.builder()
                .cityId(mumbaiCityId)
                .updates(List.of(new CityProductStockItem(productId, 10)))
                .build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                cityInventoryService.bulkUpdatePartnerCityStock(request, partnerProfile));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Access denied: You are not authorized to manage inventory for this city"));
    }

    @Test
    @DisplayName("Partner fetching assigned city inventory returns city-specific pricing and local branch stock")
    void testGetPartnerCityInventory_Success() {
        // Product has city pricing in Pune: price 4299, exchangeDiscount 600, stock 14
        ProductCityPricing punePricing = new ProductCityPricing();
        punePricing.setProduct(product);
        punePricing.setCity(puneCity);
        punePricing.setPrice(BigDecimal.valueOf(4299));
        punePricing.setExchangeDiscount(BigDecimal.valueOf(600));
        punePricing.setStock(14);
        product.getCityPrices().add(punePricing);

        when(cityRepository.findById(puneCityId)).thenReturn(Optional.of(puneCity));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<PartnerInventoryItemResponse> response = cityInventoryService.getPartnerCityInventory(puneCityId, partnerProfile);

        assertNotNull(response);
        assertEquals(1, response.size());
        PartnerInventoryItemResponse item = response.get(0);
        assertEquals(productId, item.getProductId());
        assertEquals("Exide Mileage ML38B20R", item.getProductName());
        assertEquals(BigDecimal.valueOf(4299), item.getProductPrice());
        assertEquals(BigDecimal.valueOf(600), item.getExchangeDiscount());
        assertEquals(14, item.getProductStock());
        assertEquals("Exide", item.getBrandName());
        assertEquals("35Ah", item.getCapacity());
        assertEquals("Car Battery", item.getProductCategory());
        assertEquals("https://img.batterymantra.com/exide.png", item.getProductImage());
    }

    @Test
    @DisplayName("Partner updates local branch stock for single product")
    void testUpdatePartnerProductStock_Success() {
        when(cityRepository.findById(puneCityId)).thenReturn(Optional.of(puneCity));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        PartnerInventoryItemResponse response = cityInventoryService.updatePartnerProductStock(
                productId, puneCityId, 25, partnerProfile);

        assertNotNull(response);
        assertEquals(25, response.getProductStock());
        assertEquals(1, product.getCityPrices().size());
        assertEquals(25, product.getCityPrices().get(0).getStock());
    }

    @Test
    @DisplayName("Partner bulk updates local branch stock for assigned city")
    void testBulkUpdatePartnerCityStock_Success() {
        UUID prod2Id = UUID.randomUUID();
        Product product2 = new Product();
        product2.setProductId(prod2Id);
        product2.setProductName("Amaron Current CR100");
        product2.setProductPrice(BigDecimal.valueOf(5200));
        product2.setApproved(true);
        product2.setCityPrices(new ArrayList<>());

        when(cityRepository.findById(puneCityId)).thenReturn(Optional.of(puneCity));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findById(prod2Id)).thenReturn(Optional.of(product2));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        PartnerBulkInventoryUpdateRequest request = PartnerBulkInventoryUpdateRequest.builder()
                .cityId(puneCityId)
                .updates(List.of(
                        new CityProductStockItem(productId, 10),
                        new CityProductStockItem(prod2Id, 0)
                ))
                .build();

        CityInventoryBulkUpdateResponse response = cityInventoryService.bulkUpdatePartnerCityStock(request, partnerProfile);

        assertNotNull(response);
        assertEquals(puneCityId, response.getCityId());
        assertEquals(2, response.getUpdatedCount());
        assertEquals(10, product.getCityPrices().get(0).getStock());
        assertEquals(0, product2.getCityPrices().get(0).getStock());
    }

    @Test
    @DisplayName("Admin has unrestricted access to view any city inventory")
    void testAdminGetCityInventory_Unrestricted() {
        when(cityRepository.findById(mumbaiCityId)).thenReturn(Optional.of(mumbaiCity));
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<PartnerInventoryItemResponse> response = cityInventoryService.getCityInventoryForAdmin(mumbaiCityId);

        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    @DisplayName("Admin updates stock in city_pricing table for that product & city without restriction")
    void testAdminUpdateCityProductStock_Success() {
        when(cityRepository.findById(mumbaiCityId)).thenReturn(Optional.of(mumbaiCity));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        PartnerInventoryItemResponse response = cityInventoryService.updateAdminCityProductStock(
                mumbaiCityId, productId, 25);

        assertNotNull(response);
        assertEquals(25, response.getProductStock());
        assertEquals(1, product.getCityPrices().size());
        assertEquals(25, product.getCityPrices().get(0).getStock());
    }

    @Test
    @DisplayName("Admin bulk updates stock in city_pricing table for that city")
    void testAdminBulkUpdateCityStock_Success() {
        when(cityRepository.findById(mumbaiCityId)).thenReturn(Optional.of(mumbaiCity));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        CityInventoryBulkUpdateResponse response = cityInventoryService.bulkUpdateAdminCityStock(
                mumbaiCityId, List.of(new CityProductStockItem(productId, 15)));

        assertNotNull(response);
        assertEquals(1, response.getUpdatedCount());
        assertEquals(15, product.getCityPrices().get(0).getStock());
    }

    @Test
    @DisplayName("Reject negative stock update with HTTP 400 Bad Request")
    void testNegativeStock_Rejected() {
        assertThrows(ResponseStatusException.class, () ->
                cityInventoryService.updatePartnerProductStock(productId, puneCityId, -5, partnerProfile));

        assertThrows(ResponseStatusException.class, () ->
                cityInventoryService.updateAdminCityProductStock(puneCityId, productId, -1));
    }
}

