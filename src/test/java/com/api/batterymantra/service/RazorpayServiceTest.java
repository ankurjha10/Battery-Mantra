package com.api.batterymantra.service;

import com.api.batterymantra.config.RazorpayConfig;
import com.api.batterymantra.dto.campaign.ValidateCartResponse;
import com.api.batterymantra.dto.payment.CheckoutItemSnapshot;
import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentFailedRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.RazorpayOrderResponse;
import com.api.batterymantra.dto.payment.VerifyPaymentRequest;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentCheckoutStatus;
import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.entity.enums.UserRole;
import com.api.batterymantra.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    private static final String KEY_SECRET = "test_razorpay_key_secret";
    private static final String WEBHOOK_SECRET = "test_razorpay_webhook_secret";

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private RazorpayConfig razorpayConfig;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PendingPaymentCheckoutRepository pendingPaymentCheckoutRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PartnerProfileRepository partnerProfileRepository;

    @Mock
    private PincodeRepository pincodeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private CouponService couponService;

    @Mock
    private CentralInventoryService centralInventoryService;

    @Mock
    private OrderClient orderClient;

    private ObjectMapper objectMapper;
    private RazorpayService razorpayService;

    private UUID customerId;
    private UUID addressId;
    private User customer;
    private Address address;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        razorpayClient.orders = orderClient;
        objectMapper = new ObjectMapper();

        razorpayService = new RazorpayService(
                razorpayClient,
                razorpayConfig,
                cartRepository,
                addressRepository,
                orderRepository,
                pendingPaymentCheckoutRepository,
                productRepository,
                partnerProfileRepository,
                pincodeRepository,
                cityRepository,
                userRepository,
                smsService,
                notificationService,
                campaignService,
                couponService,
                centralInventoryService,
                objectMapper
        );

        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        customer = new User();
        customer.setUserId(customerId);
        customer.setUsername("Test User");
        customer.setPhoneNumber("9876543210");

        address = new Address();
        address.setAddressId(addressId);
        address.setUser(customer);
        address.setPostalCode("110001");
        address.setCity("Delhi");
        address.setIsDeleted(false);

        product = new Product();
        product.setProductId(UUID.randomUUID());
        product.setProductName("Solar Inverter Battery 150Ah");
        product.setProductPrice(BigDecimal.valueOf(110999)); // Original price ₹1,10,999
        product.setProductStock(10);
        product.setExchangeDiscount(BigDecimal.ZERO);
        product.setAutoAssignToPartner(false);

        cartItem = new CartItem();
        cartItem.setCartItemId(UUID.randomUUID());
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setExchangeOldBattery(false);

        cart = new Cart();
        cart.setCartId(UUID.randomUUID());
        cart.setCustomer(customer);
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));
    }

    @Test
    @DisplayName("Create Razorpay Order: ₹10,000 campaign discount saved in PendingPaymentCheckout; NO order created & NO stock deducted yet")
    void testCreateOrder_WithActiveCampaignDiscount_SavesPendingCheckoutWithoutCreatingOrder() throws Exception {
        UUID campaignId = UUID.randomUUID();
        CreateRazorpayOrderRequest request = CreateRazorpayOrderRequest.builder()
                .addressId(addressId)
                .deliveryMethod(DeliveryMethod.STANDARD_DELIVERY.name())
                .selectedCity("Delhi")
                .build();

        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(cityRepository.findByCityNameIgnoreCase("Delhi")).thenReturn(Optional.empty());
        when(pincodeRepository.findByCode("110001")).thenReturn(Optional.empty());

        ValidateCartResponse campaignResponse = ValidateCartResponse.builder()
                .applicable(true)
                .campaignId(campaignId)
                .campaignName("Mega Festive Deal")
                .discountAmount(BigDecimal.valueOf(10000))
                .finalPayable(BigDecimal.valueOf(100999))
                .message("Offer applied successfully!")
                .build();

        when(campaignService.calculateCampaignDiscountForCart(any(), any(), any(), any()))
                .thenReturn(campaignResponse);

        JSONObject mockOrderJson = new JSONObject();
        mockOrderJson.put("id", "order_rzp_deal_123");
        mockOrderJson.put("amount", 10099900L);
        Order mockRazorpayOrder = new Order(mockOrderJson);

        when(orderClient.create(any(JSONObject.class))).thenReturn(mockRazorpayOrder);
        when(razorpayConfig.getKeyId()).thenReturn("rzp_test_key");
        when(pendingPaymentCheckoutRepository.save(any(PendingPaymentCheckout.class))).thenAnswer(inv -> {
            PendingPaymentCheckout p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        RazorpayOrderResponse response = razorpayService.createOrder(customerId, request);

        // 1. Razorpay API called with discounted amount in paise (10099900)
        ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(jsonCaptor.capture());
        assertEquals(10099900L, jsonCaptor.getValue().getLong("amount"));
        assertEquals("INR", jsonCaptor.getValue().getString("currency"));

        // 2. Response returns Razorpay order info and null orderId (deferred until payment succeeds)
        assertNotNull(response);
        assertEquals("order_rzp_deal_123", response.getRazorpayOrderId());
        assertEquals(10099900L, response.getAmount());
        assertNull(response.getOrderId());

        // 3. NO real Order created and NO stock deducted yet!
        verify(orderRepository, never()).save(any(Orders.class));
        verify(centralInventoryService, never()).deductStockForOrder(any(), any());
        verify(centralInventoryService, never()).deductStockForPaidOrder(any(), any());

        // 4. PendingPaymentCheckout saved with discounted amounts & campaign fields
        ArgumentCaptor<PendingPaymentCheckout> checkoutCaptor = ArgumentCaptor.forClass(PendingPaymentCheckout.class);
        verify(pendingPaymentCheckoutRepository).save(checkoutCaptor.capture());
        PendingPaymentCheckout savedCheckout = checkoutCaptor.getValue();

        assertEquals("order_rzp_deal_123", savedCheckout.getRazorpayOrderId());
        assertEquals(0, BigDecimal.valueOf(100999).compareTo(savedCheckout.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(savedCheckout.getCampaignDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(savedCheckout.getDiscountAmount()));
        assertEquals(campaignId, savedCheckout.getAppliedCampaignId());
        assertEquals("Mega Festive Deal", savedCheckout.getCampaignName());
        assertEquals(PaymentCheckoutStatus.PENDING, savedCheckout.getStatus());
        assertNotNull(savedCheckout.getExpiresAt());
        assertNotNull(savedCheckout.getItemsSnapshotJson());
    }

    @Test
    @DisplayName("Create Razorpay Order: Without campaign discount, saves PendingPaymentCheckout with full amount (11099900 paise)")
    void testCreateOrder_WithoutCampaignDiscount_SavesPendingCheckoutWithOriginalAmount() throws Exception {
        CreateRazorpayOrderRequest request = CreateRazorpayOrderRequest.builder()
                .addressId(addressId)
                .deliveryMethod(DeliveryMethod.STANDARD_DELIVERY.name())
                .selectedCity("Delhi")
                .build();

        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(cityRepository.findByCityNameIgnoreCase("Delhi")).thenReturn(Optional.empty());
        when(pincodeRepository.findByCode("110001")).thenReturn(Optional.empty());

        ValidateCartResponse noCampaignResponse = ValidateCartResponse.builder()
                .applicable(false)
                .discountAmount(BigDecimal.ZERO)
                .finalPayable(BigDecimal.valueOf(110999))
                .build();

        when(campaignService.calculateCampaignDiscountForCart(any(), any(), any(), any()))
                .thenReturn(noCampaignResponse);

        JSONObject mockOrderJson = new JSONObject();
        mockOrderJson.put("id", "order_rzp_full_456");
        mockOrderJson.put("amount", 11099900L);
        Order mockRazorpayOrder = new Order(mockOrderJson);

        when(orderClient.create(any(JSONObject.class))).thenReturn(mockRazorpayOrder);
        when(razorpayConfig.getKeyId()).thenReturn("rzp_test_key");
        when(pendingPaymentCheckoutRepository.save(any(PendingPaymentCheckout.class))).thenAnswer(inv -> {
            PendingPaymentCheckout p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        RazorpayOrderResponse response = razorpayService.createOrder(customerId, request);

        ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(jsonCaptor.capture());
        assertEquals(11099900L, jsonCaptor.getValue().getLong("amount"));
        assertEquals(11099900L, response.getAmount());

        verify(orderRepository, never()).save(any(Orders.class));
        ArgumentCaptor<PendingPaymentCheckout> checkoutCaptor = ArgumentCaptor.forClass(PendingPaymentCheckout.class);
        verify(pendingPaymentCheckoutRepository).save(checkoutCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(110999).compareTo(checkoutCaptor.getValue().getTotalAmount()));
    }

    @Test
    @DisplayName("Verify Payment: Valid signature creates CONFIRMED/PAID order, deducts stock, clears cart, marks checkout COMPLETED")
    void testVerifyPayment_ValidSignature_CreatesOrderAndDeductsStock() throws Exception {
        String rzpOrderId = "order_rzp_ok_1";
        String rzpPaymentId = "pay_ok_1";
        String validSig = hmacSha256(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);

        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.PENDING);

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(rzpOrderId);
        req.setRazorpayPaymentId(rzpPaymentId);
        req.setRazorpaySignature(validSig);

        UUID createdOrderId = UUID.randomUUID();
        when(razorpayConfig.getKeySecret()).thenReturn(KEY_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));
        when(orderRepository.findByRazorpayOrderId(rzpOrderId)).thenReturn(Optional.empty());
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            o.setOrderId(createdOrderId);
            return o;
        });
        when(centralInventoryService.deductStockForPaidOrder(any(Orders.class), eq("CUSTOMER"))).thenReturn(true);
        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of());

        PaymentVerificationResponse resp = razorpayService.verifyPayment(customerId, req);

        assertEquals(createdOrderId, resp.getOrderId());
        assertEquals("CONFIRMED", resp.getOrderStatus());
        assertEquals("PAID", resp.getPaymentStatus());

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        Orders savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.CONFIRMED, savedOrder.getOrderStatus());
        assertEquals(PaymentStatus.PAID, savedOrder.getPaymentStatus());
        assertEquals(PaymentMethod.ONLINE, savedOrder.getPaymentMethod());
        assertEquals(1, savedOrder.getOrderItems().size());

        verify(centralInventoryService).deductStockForPaidOrder(savedOrder, "CUSTOMER");
        assertTrue(cart.getCartItems().isEmpty());
        verify(cartRepository).save(cart);
        assertEquals(PaymentCheckoutStatus.COMPLETED, checkout.getStatus());
        assertEquals(createdOrderId, checkout.getCreatedOrderId());
        assertEquals(rzpPaymentId, checkout.getRazorpayPaymentId());
        verify(smsService).sendOrderPlacedSms(eq("9876543210"), eq("Test User"), eq(createdOrderId.toString()),
                any(), any(), any(), eq("Online"));
    }

    @Test
    @DisplayName("Verify Payment: Idempotent when webhook already finalized the checkout")
    void testVerifyPayment_AlreadyCompletedByWebhook_ReturnsExistingOrderIdempotently() throws Exception {
        String rzpOrderId = "order_rzp_idem_1";
        String rzpPaymentId = "pay_idem_1";
        String validSig = hmacSha256(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);

        UUID existingOrderId = UUID.randomUUID();
        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.COMPLETED);
        checkout.setCreatedOrderId(existingOrderId);

        Orders existingOrder = Orders.builder()
                .orderId(existingOrderId)
                .customer(customer)
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.ONLINE)
                .razorpayOrderId(rzpOrderId)
                .build();

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(rzpOrderId);
        req.setRazorpayPaymentId(rzpPaymentId);
        req.setRazorpaySignature(validSig);

        when(razorpayConfig.getKeySecret()).thenReturn(KEY_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));
        when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(existingOrder));

        PaymentVerificationResponse resp = razorpayService.verifyPayment(customerId, req);

        assertEquals(existingOrderId, resp.getOrderId());
        assertEquals("CONFIRMED", resp.getOrderStatus());
        assertEquals("PAID", resp.getPaymentStatus());
        verify(orderRepository, never()).save(any());
        verify(centralInventoryService, never()).deductStockForPaidOrder(any(), any());
        verify(smsService, never()).sendOrderPlacedSms(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Verify Payment: Invalid signature rejects request and NEVER creates an order or deducts stock")
    void testVerifyPayment_InvalidSignature_ThrowsBadRequestAndCreatesNoOrder() {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId("order_rzp_bad_1");
        req.setRazorpayPaymentId("pay_bad_1");
        req.setRazorpaySignature("invalid_signature_xyz");

        when(razorpayConfig.getKeySecret()).thenReturn(KEY_SECRET);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> razorpayService.verifyPayment(customerId, req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(centralInventoryService, never()).deductStockForPaidOrder(any(), any());
        verify(pendingPaymentCheckoutRepository, never()).findByRazorpayOrderIdForUpdate(any());
    }

    @Test
    @DisplayName("Verify Payment: Stock shortage at capture time still preserves PAID order and alerts admins")
    void testVerifyPayment_StockShortageAtCapture_PreservesPaidOrderAndAlertsAdmins() throws Exception {
        String rzpOrderId = "order_rzp_short_1";
        String rzpPaymentId = "pay_short_1";
        String validSig = hmacSha256(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);

        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.PENDING);

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(rzpOrderId);
        req.setRazorpayPaymentId(rzpPaymentId);
        req.setRazorpaySignature(validSig);

        User admin = new User();
        admin.setUserId(UUID.randomUUID());

        UUID createdOrderId = UUID.randomUUID();
        when(razorpayConfig.getKeySecret()).thenReturn(KEY_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));
        when(orderRepository.findByRazorpayOrderId(rzpOrderId)).thenReturn(Optional.empty());
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(createdOrderId);
            }
            return o;
        });
        when(centralInventoryService.deductStockForPaidOrder(any(Orders.class), eq("CUSTOMER"))).thenReturn(false);
        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

        PaymentVerificationResponse resp = razorpayService.verifyPayment(customerId, req);

        assertEquals(createdOrderId, resp.getOrderId());
        assertEquals("CONFIRMED", resp.getOrderStatus());
        assertEquals("PAID", resp.getPaymentStatus());
        verify(orderRepository, times(2)).save(any(Orders.class));
        verify(notificationService).sendPushNotification(
                eq(admin.getUserId()),
                eq("URGENT: Paid Order Stock Shortage"),
                contains("insufficient"),
                isNull()
        );
    }

    @Test
    @DisplayName("Mark Payment Failed: Records failure on PendingPaymentCheckout without creating any order")
    void testMarkPaymentFailed_RecordsFailureAndCreatesNoOrder() throws Exception {
        String rzpOrderId = "order_rzp_timeout_1";
        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.PENDING);

        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));

        PaymentFailedRequest req = PaymentFailedRequest.builder()
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId("pay_timeout_1")
                .reason("Customer - Payment timeout")
                .build();

        Map<String, Object> resp = razorpayService.markPaymentFailed(customerId, req);

        assertEquals("FAILED", resp.get("status"));
        assertEquals(PaymentCheckoutStatus.FAILED, checkout.getStatus());
        assertEquals("Customer - Payment timeout", checkout.getFailureReason());
        verify(pendingPaymentCheckoutRepository).save(checkout);
        verify(orderRepository, never()).save(any());
        verify(centralInventoryService, never()).deductStockForPaidOrder(any(), any());
    }

    @Test
    @DisplayName("Webhook payment.failed (Customer - Payment timeout): Marks checkout FAILED, creates NO order, deducts NO stock")
    void testHandleWebhook_PaymentFailedTimeout_DoesNotCreateOrder() throws Exception {
        String rzpOrderId = "order_rzp_wh_fail_1";
        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.PENDING);

        String rawPayload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_wh_fail_1",
                        "order_id": "order_rzp_wh_fail_1",
                        "error_description": "Customer - Payment timeout",
                        "error_reason": "payment_timed_out",
                        "error_source": "customer"
                      }
                    }
                  }
                }
                """;
        String webhookSig = hmacSha256(rawPayload, WEBHOOK_SECRET);

        when(razorpayConfig.getWebhookSecret()).thenReturn(WEBHOOK_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));

        Map<String, Object> resp = razorpayService.handleWebhook(rawPayload, webhookSig);

        assertEquals("recorded", resp.get("status"));
        assertEquals(PaymentCheckoutStatus.FAILED, checkout.getStatus());
        assertEquals("Customer - Payment timeout", checkout.getFailureReason());
        assertEquals("pay_wh_fail_1", checkout.getRazorpayPaymentId());
        verify(orderRepository, never()).save(any());
        verify(centralInventoryService, never()).deductStockForPaidOrder(any(), any());
    }

    @Test
    @DisplayName("Webhook payment.captured: Creates real CONFIRMED/PAID order even if customer closed browser before /verify")
    void testHandleWebhook_PaymentCaptured_CreatesOrderFromPendingCheckout() throws Exception {
        String rzpOrderId = "order_rzp_wh_ok_1";
        PendingPaymentCheckout checkout = buildPendingCheckout(rzpOrderId, PaymentCheckoutStatus.PENDING);

        String rawPayload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_wh_ok_1",
                        "order_id": "order_rzp_wh_ok_1",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;
        String webhookSig = hmacSha256(rawPayload, WEBHOOK_SECRET);

        UUID createdOrderId = UUID.randomUUID();
        when(razorpayConfig.getWebhookSecret()).thenReturn(WEBHOOK_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.of(checkout));
        when(orderRepository.findByRazorpayOrderId(rzpOrderId)).thenReturn(Optional.empty());
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            o.setOrderId(createdOrderId);
            return o;
        });
        when(centralInventoryService.deductStockForPaidOrder(any(Orders.class), eq("RAZORPAY_WEBHOOK"))).thenReturn(true);
        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of());

        Map<String, Object> resp = razorpayService.handleWebhook(rawPayload, webhookSig);

        assertEquals("processed", resp.get("status"));
        assertEquals(createdOrderId.toString(), resp.get("orderId"));
        assertEquals(PaymentCheckoutStatus.COMPLETED, checkout.getStatus());
        assertEquals(createdOrderId, checkout.getCreatedOrderId());
        verify(orderRepository).save(any(Orders.class));
        verify(centralInventoryService).deductStockForPaidOrder(any(Orders.class), eq("RAZORPAY_WEBHOOK"));
    }

    @Test
    @DisplayName("Webhook invalid signature: Rejects with 400 BAD_REQUEST and creates NO order")
    void testHandleWebhook_InvalidSignature_ThrowsBadRequest() {
        when(razorpayConfig.getWebhookSecret()).thenReturn(WEBHOOK_SECRET);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> razorpayService.handleWebhook("{\"event\":\"payment.captured\"}", "tampered_sig"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify Payment: Legacy fallback confirms pre-deployment PENDING order when no PendingPaymentCheckout exists")
    void testVerifyPayment_LegacyFallback_ConfirmsExistingPendingOrder() throws Exception {
        String rzpOrderId = "order_rzp_legacy_1";
        String rzpPaymentId = "pay_legacy_1";
        String validSig = hmacSha256(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);

        Orders legacyOrder = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(customer)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.ONLINE)
                .razorpayOrderId(rzpOrderId)
                .totalAmount(BigDecimal.valueOf(5000))
                .build();

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(rzpOrderId);
        req.setRazorpayPaymentId(rzpPaymentId);
        req.setRazorpaySignature(validSig);

        when(razorpayConfig.getKeySecret()).thenReturn(KEY_SECRET);
        when(pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(rzpOrderId))
                .thenReturn(Optional.empty());
        when(orderRepository.findByRazorpayOrderId(rzpOrderId)).thenReturn(Optional.of(legacyOrder));
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));

        PaymentVerificationResponse resp = razorpayService.verifyPayment(customerId, req);

        assertEquals(legacyOrder.getOrderId(), resp.getOrderId());
        assertEquals("CONFIRMED", resp.getOrderStatus());
        assertEquals("PAID", resp.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, legacyOrder.getOrderStatus());
        assertEquals(PaymentStatus.PAID, legacyOrder.getPaymentStatus());
    }

    private PendingPaymentCheckout buildPendingCheckout(String rzpOrderId, PaymentCheckoutStatus status) throws Exception {
        CheckoutItemSnapshot snap = CheckoutItemSnapshot.builder()
                .productId(product.getProductId())
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(110999))
                .exchangeOldBattery(false)
                .exchangeDiscount(BigDecimal.ZERO)
                .build();

        return PendingPaymentCheckout.builder()
                .id(UUID.randomUUID())
                .razorpayOrderId(rzpOrderId)
                .customer(customer)
                .shippingAddress(address)
                .deliveryMethod(DeliveryMethod.STANDARD_DELIVERY)
                .totalAmount(BigDecimal.valueOf(110999))
                .exchangeDiscount(BigDecimal.ZERO)
                .selectedCity("Delhi")
                .campaignDiscountAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .itemsSnapshotJson(objectMapper.writeValueAsString(List.of(snap)))
                .status(status)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
