package com.api.batterymantra.service;

import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.repository.OrderRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.util.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServicePartnerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CentralInventoryService centralInventoryService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SmsService smsService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID partnerId;
    private UUID orderId;
    private PartnerProfile partnerProfile;
    private Orders order;

    @BeforeEach
    void setUp() {
        partnerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        partnerProfile = PartnerProfile.builder()
                .id(partnerId)
                .businessName("Test Partner Battery Hub")
                .build();

        User customer = User.builder()
                .userId(UUID.randomUUID())
                .username("john_doe")
                .phoneNumber("9876543210")
                .build();

        order = Orders.builder()
                .orderId(orderId)
                .customer(customer)
                .assignedPartner(partnerProfile)
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(3500))
                .deliveryMethod(DeliveryMethod.STANDARD_DELIVERY)
                .build();
    }

    @Test
    @DisplayName("Transition PENDING -> PROCESSING succeeds")
    void testTransition_PendingToProcessing_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        expectedResponse.setOrderId(orderId);
        expectedResponse.setOrderStatus(OrderStatus.PROCESSING.name());
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updatePartnerOrderStatus(orderId, OrderStatus.PROCESSING, partnerId);

        assertNotNull(response);
        assertEquals(OrderStatus.PROCESSING.name(), response.getOrderStatus());
        assertEquals(OrderStatus.PROCESSING, order.getOrderStatus());
    }

    @Test
    @DisplayName("Transition CONFIRMED -> PROCESSING succeeds")
    void testTransition_ConfirmedToProcessing_Success() {
        order.setOrderStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updatePartnerOrderStatus(orderId, OrderStatus.PROCESSING, partnerId);

        assertNotNull(response);
        assertEquals(OrderStatus.PROCESSING, order.getOrderStatus());
    }

    @Test
    @DisplayName("Transition PENDING -> SHIPPED is rejected")
    void testTransition_PendingToShipped_Rejected() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.updatePartnerOrderStatus(orderId, OrderStatus.SHIPPED, partnerId));

        assertTrue(exception.getMessage().contains("Invalid status transition from PENDING to SHIPPED"));
    }

    @Test
    @DisplayName("Transition PROCESSING -> SHIPPED rejects if engineer is null")
    void testTransition_ProcessingToShipped_RejectsIfEngineerNull() {
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setAssignedEngineer(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.updatePartnerOrderStatus(orderId, OrderStatus.SHIPPED, partnerId));

        assertTrue(exception.getMessage().contains("without assigning a Field Engineer"));
    }

    @Test
    @DisplayName("Transition PROCESSING -> SHIPPED succeeds when engineer is assigned")
    void testTransition_ProcessingToShipped_SuccessWithEngineer() {
        order.setOrderStatus(OrderStatus.PROCESSING);
        EngineerProfile engineer = EngineerProfile.builder()
                .id(UUID.randomUUID())
                .firstName("Technician")
                .lastName("Kumar")
                .user(User.builder().phoneNumber("9123456780").build())
                .build();
        order.setAssignedEngineer(engineer);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updatePartnerOrderStatus(orderId, OrderStatus.SHIPPED, partnerId);

        assertNotNull(response);
        assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
        assertNotNull(order.getDeliverySecurityCode(), "Delivery security code should be present on SHIPPED");
    }

    @Test
    @DisplayName("Transition SHIPPED -> DELIVERED succeeds")
    void testTransition_ShippedToDelivered_Success() {
        order.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updatePartnerOrderStatus(orderId, OrderStatus.DELIVERED, partnerId);

        assertNotNull(response);
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        assertNotNull(order.getCompletedAt());
    }

    @Test
    @DisplayName("Transition * -> CANCELLED saves reason and sets cancelledBy to PARTNER")
    void testTransition_CancelOrder_SetsPartnerAndReason() {
        order.setOrderStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        String reason = "Customer battery model mismatch at doorstep";
        orderService.updatePartnerOrderStatus(orderId, OrderStatus.CANCELLED, reason, partnerId);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals("PARTNER", order.getCancelledBy());
        assertEquals(reason, order.getCancellationReason());
        assertNotNull(order.getCompletedAt());
        verify(centralInventoryService).restoreStockForOrder(order, "PARTNER", reason);
    }

    @Test
    @DisplayName("Modifying already DELIVERED order is rejected")
    void testModifyDeliveredOrder_Rejected() {
        order.setOrderStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.updatePartnerOrderStatus(orderId, OrderStatus.CANCELLED, partnerId));

        assertTrue(exception.getMessage().contains("Cannot change status of a delivered order"));
    }

    @Test
    @DisplayName("Modifying already CANCELLED order is rejected")
    void testModifyCancelledOrder_Rejected() {
        order.setOrderStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.updatePartnerOrderStatus(orderId, OrderStatus.PROCESSING, partnerId));

        assertTrue(exception.getMessage().contains("Cannot change status of a cancelled order"));
    }

    @Test
    @DisplayName("Reject status update if order is not assigned to this partner")
    void testStatusUpdate_ForbiddenForUnassignedPartner() {
        UUID otherPartnerId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class, () ->
                orderService.updatePartnerOrderStatus(orderId, OrderStatus.PROCESSING, otherPartnerId));
    }

    @Test
    @DisplayName("Append internal engineer notes on order")
    void testAddPartnerOrderNote_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse expectedResponse = new OrderResponse();
        when(orderMapper.toOrderResponse(any(Orders.class))).thenReturn(expectedResponse);

        // First note
        orderService.addPartnerOrderNote(orderId, "Customer requested delivery after 4 PM", partnerId);
        assertEquals("Customer requested delivery after 4 PM", order.getEngineerNotes());

        // Second note (appends)
        orderService.addPartnerOrderNote(orderId, "Call before arriving", partnerId);
        assertEquals("Customer requested delivery after 4 PM\nCall before arriving", order.getEngineerNotes());
    }

    @Test
    @DisplayName("Empty order note is rejected")
    void testAddEmptyOrderNote_Rejected() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class, () ->
                orderService.addPartnerOrderNote(orderId, "   ", partnerId));
    }
}
