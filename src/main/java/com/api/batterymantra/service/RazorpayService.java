package com.api.batterymantra.service;

import com.api.batterymantra.config.RazorpayConfig;
import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.RazorpayOrderResponse;
import com.api.batterymantra.dto.payment.VerifyPaymentRequest;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PincodeRepository pincodeRepository;
    private final SmsService smsService;

    @Transactional
    public RazorpayOrderResponse createOrder(UUID customerId, CreateRazorpayOrderRequest request) {
        // 1. Fetch the cart
        Cart cart = cartRepository.findByUserId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cart not found for user: " + customerId));

        List<CartItem> cartItemList = cart.getCartItems();
        if (cartItemList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cart is empty for user: " + customerId);
        }

        // 2. Validate stock availability
        for (CartItem cartItem : cartItemList) {
            Product product = cartItem.getProduct();
            if (product.getProductStock() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product: " + product.getProductName());
            }
        }

        // 3. Fetch and validate the address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Address not found for user: " + customerId));

        if (address.getIsDeleted() != null && address.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot use a deleted address for checkout");
        }

        if (!address.getUser().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use a valid address");
        }

        // 4. Parse delivery method
        DeliveryMethod deliveryMethod;
        try {
            deliveryMethod = DeliveryMethod.valueOf(request.getDeliveryMethod());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid or missing Delivery Method");
        }

        // 5. Calculate total amount
        List<OrderItems> orderItems = new ArrayList<>();
        boolean shouldAutoAssign = false;

        for (CartItem cartItem : cartItemList) {
            OrderItems items = OrderItems.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getProduct().getProductPrice())
                    .build();

            if (cartItem.getProduct().isAutoAssignToPartner()) {
                shouldAutoAssign = true;
            }

            orderItems.add(items);
        }

        BigDecimal subTotal = orderItems.stream()
                .map(item -> item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal exchangeDiscount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItemList) {
            if (cartItem.isExchangeOldBattery()
                    && cartItem.getProduct().getExchangeDiscount() != null) {
                exchangeDiscount = exchangeDiscount.add(
                        cartItem.getProduct().getExchangeDiscount()
                                .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }

        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 6. Create Razorpay order
        long amountInPaise = total.multiply(BigDecimal.valueOf(100)).longValue();

        JSONObject razorpayOrderRequest = new JSONObject();
        razorpayOrderRequest.put("amount", amountInPaise);
        razorpayOrderRequest.put("currency", "INR");
        razorpayOrderRequest.put("receipt", "bm_" + UUID.randomUUID().toString().substring(0, 8));

        Order razorpayOrder;
        try {
            razorpayOrder = razorpayClient.orders.create(razorpayOrderRequest);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create payment order: " + e.getMessage());
        }

        String razorpayOrderId = razorpayOrder.get("id");

        // 7. Create internal order with PENDING status
        Orders orders = Orders.builder()
                .customer(cart.getCustomer())
                .shippingAddress(address)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .deliveryMethod(deliveryMethod)
                .paymentMethod(PaymentMethod.ONLINE)
                .installationDate(request.getInstallationDate())
                .razorpayOrderId(razorpayOrderId)
                .totalAmount(total)
                .exchangeDiscount(exchangeDiscount)
                .build();

        // 8. Auto-assign partner (same logic as COD checkout)
        if (shouldAutoAssign) {
            PartnerProfile matchedPartner = null;

            if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
                String cleanPincode = address.getPostalCode().trim();
                var pincodeOpt = pincodeRepository.findByCode(cleanPincode);
                if (pincodeOpt.isPresent() && pincodeOpt.get().getCity() != null) {
                    UUID cityId = pincodeOpt.get().getCity().getCityId();
                    matchedPartner = partnerProfileRepository
                            .findFirstByIsActiveTrueAndOperatingCities_CityId(cityId)
                            .orElse(null);
                }
            }

            if (matchedPartner == null && address.getCity() != null
                    && !address.getCity().isBlank()) {
                String cleanCity = address.getCity().trim();
                matchedPartner = partnerProfileRepository
                        .findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(cleanCity)
                        .orElse(null);
            }

            if (matchedPartner != null) {
                orders.setAssignedPartner(matchedPartner);
            }
        }

        // 9. Link order items and reduce stock
        for (int i = 0; i < orderItems.size(); i++) {
            orderItems.get(i).setOrder(orders);

            // Reduce stock
            CartItem cartItem = cartItemList.get(i);
            Product product = cartItem.getProduct();
            product.setProductStock(product.getProductStock() - cartItem.getQuantity());
            productRepository.save(product);
        }
        orders.setOrderItems(orderItems);

        // 10. Save the order — DO NOT clear cart yet
        Orders savedOrder = orderRepository.save(orders);

        log.info("Razorpay order created: {} for internal order: {}",
                razorpayOrderId, savedOrder.getOrderId());

        return new RazorpayOrderResponse(
                razorpayOrderId,
                amountInPaise,
                "INR",
                razorpayConfig.getKeyId(),
                savedOrder.getOrderId()
        );
    }

    @Transactional
    public PaymentVerificationResponse verifyPayment(UUID customerId,
                                                      VerifyPaymentRequest request) {
        // 1. Fetch the order by Razorpay order ID
        Orders order = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found for Razorpay order ID: "
                                + request.getRazorpayOrderId()));

        // 2. Validate ownership
        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to verify this payment");
        }

        // 3. Verify HMAC-SHA256 signature
        String payload = request.getRazorpayOrderId() + "|"
                + request.getRazorpayPaymentId();
        String expectedSignature = calculateHmacSha256(payload,
                razorpayConfig.getKeySecret());

        if (expectedSignature != null
                && expectedSignature.equals(request.getRazorpaySignature())) {
            // Payment verified successfully
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            // Clear the user's cart
            Cart cart = cartRepository.findByUserId(customerId).orElse(null);
            if (cart != null) {
                cart.getCartItems().clear();
                cartRepository.save(cart);
            }

            // Send SMS notifications
            String customerPhone = order.getCustomer().getPhoneNumber();
            String customerName = order.getCustomer().getUsername();
            String orderIdStr = order.getOrderId().toString();

            if (customerPhone != null && !customerPhone.isBlank()) {
                smsService.sendOrderPlacedSms(customerPhone, customerName, orderIdStr);
            }
            smsService.sendAdminOrderAlert("ADMIN", orderIdStr);

            log.info("Payment verified for order: {}", order.getOrderId());

            return new PaymentVerificationResponse(
                    order.getOrderId(),
                    order.getOrderStatus().name(),
                    order.getPaymentStatus().name(),
                    "Payment verified successfully"
            );
        } else {
            // Invalid signature — payment failed
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.warn("Payment verification failed for order: {}", order.getOrderId());

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment verification failed. Invalid signature.");
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating HMAC-SHA256: {}", e.getMessage(), e);
            return null;
        }
    }
}
