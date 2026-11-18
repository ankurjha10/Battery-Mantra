package com.api.batterymantra.service;

import com.api.batterymantra.entity.Orders;
import com.api.batterymantra.entity.PendingPaymentCheckout;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentCheckoutStatus;
import com.api.batterymantra.repository.OrderRepository;
import com.api.batterymantra.repository.PendingPaymentCheckoutRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCronServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PendingPaymentCheckoutRepository pendingPaymentCheckoutRepository;

    @Mock
    private CentralInventoryService centralInventoryService;

    @InjectMocks
    private OrderCronService orderCronService;

    @Test
    @DisplayName("Expire Abandoned Checkouts: Marks expired PENDING checkouts as EXPIRED without touching orders or stock")
    void testExpireAbandonedPendingCheckouts_MarksExpiredWithoutTouchingOrdersOrStock() {
        PendingPaymentCheckout c1 = PendingPaymentCheckout.builder()
                .id(UUID.randomUUID())
                .razorpayOrderId("order_rzp_exp_1")
                .status(PaymentCheckoutStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(pendingPaymentCheckoutRepository.findByStatusAndExpiresAtBefore(
                eq(PaymentCheckoutStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(c1));

        orderCronService.expireAbandonedPendingCheckouts();

        assertEquals(PaymentCheckoutStatus.EXPIRED, c1.getStatus());
        assertNotNull(c1.getFailureReason());
        assertTrue(c1.getFailureReason().contains("Payment timeout"));
        verify(pendingPaymentCheckoutRepository).saveAll(List.of(c1));
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(centralInventoryService);
    }

    @Test
    @DisplayName("Auto-cancel Old Pending Orders: Cancels 10+ day old PENDING orders and restores stock")
    void testAutoCancelOldPendingOrders_CancelsAndRestoresStock() {
        Orders oldOrder = Orders.builder()
                .orderId(UUID.randomUUID())
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByOrderStatusAndPlacedAtBefore(eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(oldOrder));

        orderCronService.autoCancelOldPendingOrders();

        assertEquals(OrderStatus.CANCELLED, oldOrder.getOrderStatus());
        verify(orderRepository).save(oldOrder);
        verify(centralInventoryService).restoreStockForOrder(eq(oldOrder), eq("SYSTEM"), any());
    }
}
