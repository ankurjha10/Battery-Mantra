package com.api.batterymantra.service;

import com.api.batterymantra.config.RazorpayConfig;
import com.api.batterymantra.dto.campaign.ValidateCartResponse;
import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.RazorpayOrderResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.repository.*;
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

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

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
    private ProductRepository productRepository;

    @Mock
    private PartnerProfileRepository partnerProfileRepository;

    @Mock
    private PincodeRepository pincodeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private CouponService couponService;

    @Mock
    private CentralInventoryService centralInventoryService;

    @Mock
    private OrderClient orderClient;

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

        razorpayService = new RazorpayService(
                razorpayClient,
                razorpayConfig,
                cartRepository,
                addressRepository,
                orderRepository,
                productRepository,
                partnerProfileRepository,
                pincodeRepository,
                cityRepository,
                smsService,
                campaignService,
                couponService,
                centralInventoryService
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
    @DisplayName("Create Razorpay Order: ₹10,000 campaign discount is deducted from ₹1,10,999 -> creates order with 10099900 paise (₹1,00,999)")
    void testCreateOrder_WithActiveCampaignDiscount_CreatesRazorpayOrderWithDiscountedAmount() throws Exception {
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

        // ₹10,000 campaign discount returned by CampaignService
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

        // Mock Razorpay API response
        JSONObject mockOrderJson = new JSONObject();
        mockOrderJson.put("id", "order_rzp_deal_123");
        mockOrderJson.put("amount", 10099900L);
        Order mockRazorpayOrder = new Order(mockOrderJson);

        when(orderClient.create(any(JSONObject.class))).thenReturn(mockRazorpayOrder);
        when(razorpayConfig.getKeyId()).thenReturn("rzp_test_key");
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            o.setOrderId(UUID.randomUUID());
            return o;
        });

        RazorpayOrderResponse response = razorpayService.createOrder(customerId, request);

        // 1. Verify Razorpay API was called with the discounted amount in paise (10099900)
        ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(jsonCaptor.capture());
        JSONObject sentJson = jsonCaptor.getValue();
        assertEquals(10099900L, sentJson.getLong("amount"), "Razorpay order must be created with ₹1,00,999 in paise (10099900)");
        assertEquals("INR", sentJson.getString("currency"));

        // 2. Verify returned response has discounted amount
        assertNotNull(response);
        assertEquals("order_rzp_deal_123", response.getRazorpayOrderId());
        assertEquals(10099900L, response.getAmount());

        // 3. Verify internal order saved in DB has discounted totalAmount and campaign fields
        ArgumentCaptor<Orders> ordersCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(ordersCaptor.capture());
        Orders savedOrder = ordersCaptor.getValue();

        assertEquals(0, BigDecimal.valueOf(100999).compareTo(savedOrder.getTotalAmount()), "Saved order totalAmount must be ₹1,00,999");
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(savedOrder.getCampaignDiscountAmount()), "campaignDiscountAmount must be ₹10,000");
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(savedOrder.getDiscountAmount()), "discountAmount must be ₹10,000");
        assertEquals(campaignId, savedOrder.getAppliedCampaignId());
        assertEquals("Mega Festive Deal", savedOrder.getCampaignName());
        assertEquals(PaymentMethod.ONLINE, savedOrder.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, savedOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Create Razorpay Order: Without campaign discount, creates order with full un-discounted amount (11099900 paise)")
    void testCreateOrder_WithoutCampaignDiscount_CreatesRazorpayOrderWithOriginalAmount() throws Exception {
        CreateRazorpayOrderRequest request = CreateRazorpayOrderRequest.builder()
                .addressId(addressId)
                .deliveryMethod(DeliveryMethod.STANDARD_DELIVERY.name())
                .selectedCity("Delhi")
                .build();

        when(cartRepository.findByUserId(customerId)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(cityRepository.findByCityNameIgnoreCase("Delhi")).thenReturn(Optional.empty());
        when(pincodeRepository.findByCode("110001")).thenReturn(Optional.empty());

        // No campaign applicable
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
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            o.setOrderId(UUID.randomUUID());
            return o;
        });

        RazorpayOrderResponse response = razorpayService.createOrder(customerId, request);

        ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(jsonCaptor.capture());
        assertEquals(11099900L, jsonCaptor.getValue().getLong("amount"));
        assertEquals(11099900L, response.getAmount());

        ArgumentCaptor<Orders> ordersCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(ordersCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(110999).compareTo(ordersCaptor.getValue().getTotalAmount()));
    }
}

