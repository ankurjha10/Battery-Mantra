package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.CreateProductRequest;
import com.api.batterymantra.dto.product.ProductDetailResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceProposalTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CityRepository cityRepository;
    @Mock private BulkPricingService bulkPricingService;
    @Mock private SpecUnitRepository specUnitRepository;
    @Mock private CentralInventoryService centralInventoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("addProductByPartner persists proposalReason, highlights, warranty, batteryTechnology, rlCode, searchKeywords")
    void testAddProductByPartner_WithProposalFields() {
        UUID categoryId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCategoryName("Car Battery");

        Brand brand = new Brand();
        brand.setBrandId(brandId);
        brand.setBrandName("Amaron");

        PartnerProfile partner = PartnerProfile.builder()
                .id(UUID.randomUUID())
                .businessName("Pune Power Hub")
                .build();

        CreateProductRequest req = new CreateProductRequest();
        req.setProductName("Amaron Pro AP-BTZ5L");
        req.setCategoryId(categoryId);
        req.setBrandId(brandId);
        req.setProductImage("https://batterymantra.com/uploads/products/amaron.webp");
        req.setProductPrice(BigDecimal.valueOf(5200));
        req.setProductStock(10);
        req.setProposalReason("High local demand for Suzuki Swift 2024");
        req.setHighlights("Zero maintenance, High cranking power");
        req.setWarranty("55 Months (30 Free + 25 Pro-Rata)");
        req.setBatteryTechnology("AGM VRLA");
        req.setRlCode("L");
        req.setSearchKeywords("amaron, swift, 35ah, agm");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getProductId() == null) {
                p.setProductId(productId);
            }
            return p;
        });
        when(productRepository.findById(productId)).thenAnswer(inv -> {
            Product saved = new Product();
            saved.setProductId(productId);
            saved.setProductName(req.getProductName());
            saved.setProductCategory(category);
            saved.setBrand(brand);
            saved.setProductImage(req.getProductImage());
            saved.setProductPrice(req.getProductPrice());
            saved.setProductStock(req.getProductStock());
            saved.setProposalReason(req.getProposalReason());
            saved.setHighlights(req.getHighlights());
            saved.setWarranty(req.getWarranty());
            saved.setBatteryTechnology(req.getBatteryTechnology());
            saved.setRlCode(req.getRlCode());
            saved.setSearchKeywords(req.getSearchKeywords());
            return Optional.of(saved);
        });

        ProductDetailResponse response = productService.addProductByPartner(req, partner);

        assertFalse(response.isApproved());
        assertEquals(partner.getId(), response.getCreatedByPartnerId());
        assertEquals("High local demand for Suzuki Swift 2024", response.getProposalReason());
        assertEquals("Zero maintenance, High cranking power", response.getHighlights());
        assertEquals("55 Months (30 Free + 25 Pro-Rata)", response.getWarranty());
        assertEquals("AGM VRLA", response.getBatteryTechnology());
        assertEquals("L", response.getRlCode());
        assertEquals("amaron, swift, 35ah, agm", response.getSearchKeywords());
    }
}

