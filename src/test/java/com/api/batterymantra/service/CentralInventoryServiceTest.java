package com.api.batterymantra.service;

import com.api.batterymantra.dto.inventory.*;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.StockStatus;
import com.api.batterymantra.exception.InsufficientStockException;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.StockAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralInventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockAuditLogRepository stockAuditLogRepository;

    @InjectMocks
    private CentralInventoryService inventoryService;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new Product();
        product.setProductId(productId);
        product.setProductName("Exide Mileage MLD35L");
        product.setProductStock(15);
        product.setLowStockThreshold(10);
        product.setProductPrice(BigDecimal.valueOf(4500));
    }

    @Test
    void shouldReturnInventoryDashboardWithSummaryAndItems() {
        when(productRepository.countTotalSkus()).thenReturn(50L);
        when(productRepository.countInStock()).thenReturn(40L);
        when(productRepository.countLowStock()).thenReturn(7L);
        when(productRepository.countOutOfStock()).thenReturn(3L);

        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        InventoryDashboardResponse response = inventoryService.getInventoryDashboard(
                StockStatus.ALL, null, null, null, 0, 10);

        assertNotNull(response);
        assertEquals(50L, response.getSummary().getTotalSkus());
        assertEquals(40L, response.getSummary().getInStockCount());
        assertEquals(7L, response.getSummary().getLowStockCount());
        assertEquals(3L, response.getSummary().getOutOfStockCount());
        assertEquals(1, response.getItems().getContent().size());
        assertEquals("Exide Mileage MLD35L", response.getItems().getContent().get(0).getProductName());
        assertEquals(StockStatus.IN_STOCK, response.getItems().getContent().get(0).getStockStatus());
    }

    @Test
    void shouldUpdateProductStockManuallyAndLogAudit() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        UpdateStockRequest req = new UpdateStockRequest(25, "Restocked consignment");
        UpdateStockResponse resp = inventoryService.updateProductStock(productId, req, "admin@batterymantra.com");

        assertEquals(25, resp.getProductStock());
        assertEquals(StockStatus.IN_STOCK, resp.getStockStatus());
        assertEquals(25, product.getProductStock());

        verify(productRepository).save(product);
        verify(stockAuditLogRepository).save(any(StockAuditLog.class));
    }

    @Test
    void shouldRejectNegativeStockQuantityInManualUpdate() {
        UpdateStockRequest req = new UpdateStockRequest(-5, "Invalid");

        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.updateProductStock(productId, req, "admin@batterymantra.com"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldBulkUpdateStockAcrossMultipleProducts() {
        UUID prod2Id = UUID.randomUUID();
        Product prod2 = new Product();
        prod2.setProductId(prod2Id);
        prod2.setProductName("Amaron Pro 42Ah");
        prod2.setProductStock(5);

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(productRepository.findByIdWithLock(prod2Id)).thenReturn(Optional.of(prod2));

        List<BulkStockUpdateRequest> reqs = List.of(
                new BulkStockUpdateRequest(productId, 20, "Adjust 1"),
                new BulkStockUpdateRequest(prod2Id, 0, "Out of stock")
        );

        BulkStockUpdateResponse resp = inventoryService.bulkUpdateStock(reqs, "admin@batterymantra.com");

        assertEquals(2, resp.getUpdatedCount());
        assertEquals(20, product.getProductStock());
        assertEquals(0, prod2.getProductStock());
        verify(productRepository, times(2)).save(any(Product.class));
        verify(stockAuditLogRepository, times(2)).save(any(StockAuditLog.class));
    }

    @Test
    void shouldDeductStockForOrderWhenStockIsSufficient() {
        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID());

        OrderItems item = new OrderItems();
        item.setProduct(product);
        item.setQuantity(3);
        order.setOrderItems(List.of(item));

        when(stockAuditLogRepository.existsByOrderIdAndChangeType(order.getOrderId(), "ORDER_DEDUCT")).thenReturn(false);
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        inventoryService.deductStockForOrder(order, "CUSTOMER");

        assertEquals(12, product.getProductStock());
        verify(productRepository).save(product);
        verify(stockAuditLogRepository).save(argThat(log ->
                log.getChangeType().equals("ORDER_DEDUCT")
                        && log.getQuantityChange() == -3
                        && log.getPreviousStock() == 15
                        && log.getNewStock() == 12
        ));
    }

    @Test
    void shouldThrowInsufficientStockExceptionWhenStockIsInsufficient() {
        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID());

        OrderItems item = new OrderItems();
        item.setProduct(product);
        item.setQuantity(20); // only 15 available
        order.setOrderItems(List.of(item));

        when(stockAuditLogRepository.existsByOrderIdAndChangeType(order.getOrderId(), "ORDER_DEDUCT")).thenReturn(false);
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                inventoryService.deductStockForOrder(order, "CUSTOMER"));

        assertEquals(productId, ex.getProductId());
        assertEquals(15, ex.getAvailableStock());
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRestoreStockForCancelledOrder() {
        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID());
        order.setStockRestored(false);

        product.setProductStock(10);
        OrderItems item = new OrderItems();
        item.setProduct(product);
        item.setQuantity(2);
        order.setOrderItems(List.of(item));

        when(stockAuditLogRepository.existsByOrderIdAndChangeType(order.getOrderId(), "ORDER_RESTORE")).thenReturn(false);
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        inventoryService.restoreStockForOrder(order, "CUSTOMER", "Customer cancelled");

        assertEquals(12, product.getProductStock());
        assertTrue(order.isStockRestored());
        verify(productRepository).save(product);
        verify(stockAuditLogRepository).save(argThat(log ->
                log.getChangeType().equals("ORDER_RESTORE")
                        && log.getQuantityChange() == 2
                        && log.getPreviousStock() == 10
                        && log.getNewStock() == 12
        ));
    }

    @Test
    void shouldNotRestoreStockTwiceWhenAlreadyRestored() {
        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID());
        order.setStockRestored(true); // already restored

        OrderItems item = new OrderItems();
        item.setProduct(product);
        item.setQuantity(2);
        order.setOrderItems(List.of(item));

        inventoryService.restoreStockForOrder(order, "SYSTEM", "Duplicate webhook");

        verify(productRepository, never()).findByIdWithLock(any());
        verify(productRepository, never()).save(any());
    }
}

