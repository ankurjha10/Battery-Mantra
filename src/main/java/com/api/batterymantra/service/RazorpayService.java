package com.api.batterymantra.service;

import com.api.batterymantra.config.RazorpayConfig;
import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.QrCodeResponse;
import com.api.batterymantra.dto.payment.RazorpayOrderResponse;
import com.api.batterymantra.dto.payment.VerifyPaymentRequest;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.repository.*;
import com.razorpay.Order;
import com.razorpay.QrCode;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

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
                smsService.sendOrderPlacedSms(customerPhone, customerName, orderIdStr, String.valueOf(order.getTotalAmount()), order.getPlacedAt() != null ? order.getPlacedAt().toString() : "", "Your Product", "Online");
            }
            // smsService.sendAdminOrderAlert("ADMIN", orderIdStr);

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

    /**
     * Generates a Razorpay UPI QR Code for an existing COD order.
     * The QR code is single-use, fixed-amount, and expires in 30 minutes.
     */
    @Transactional
    public QrCodeResponse generateQrCode(UUID orderId) {
        // 1. Fetch the order
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        // 2. If already paid, reject the request
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is already paid. Cannot generate QR code.");
        }

        // 3. Build the Razorpay QR code request
        long amountInPaise = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100)).longValue();

        // close_by = current time + 30 minutes (Unix timestamp in seconds)
        long closeBy = Instant.now().plusSeconds(30 * 60).getEpochSecond();

        JSONObject qrRequest = new JSONObject();
        qrRequest.put("type", "upi_qr");
        qrRequest.put("name", "Order " + order.getOrderId());
        qrRequest.put("usage", "single_use");
        qrRequest.put("fixed_amount", true);
        qrRequest.put("payment_amount", amountInPaise);
        qrRequest.put("description", "Payment for Order " + order.getOrderId());
        qrRequest.put("close_by", closeBy);

        JSONObject notes = new JSONObject();
        notes.put("order_id", order.getOrderId().toString());
        qrRequest.put("notes", notes);

        // 4. Call Razorpay to create the QR code
        QrCode qrCode;
        try {
            qrCode = razorpayClient.qrCode.create(qrRequest);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay QR code for order {}: {}",
                    orderId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate QR code: " + e.getMessage());
        }

        // 5. Extract response fields
        String qrCodeId = qrCode.get("id");
        String imageUrl = qrCode.get("image_url");

        // 6. Persist QR code details on the order
        order.setQrCodeId(qrCodeId);
        order.setQrCodeImageUrl(imageUrl);
        orderRepository.save(order);

        log.info("QR code generated for order {}: qrCodeId={}, imageUrl={}",
                orderId, qrCodeId, imageUrl);

        return new QrCodeResponse(qrCodeId, imageUrl);
    }

    /**
     * Checks the payment status of a QR code for the given order.
     * If payment has been received, updates the order to PAID / ONLINE.
     */
    @Transactional
    public Map<String, Object> checkQrPaymentStatus(UUID orderId) {
        // 1. Fetch the order
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        // 2. If already paid, return immediately
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return Map.of(
                    "orderId", order.getOrderId().toString(),
                    "paymentStatus", "PAID",
                    "message", "Payment already received."
            );
        }

        // 3. Ensure a QR code was generated for this order
        String qrCodeId = order.getQrCodeId();
        if (qrCodeId == null || qrCodeId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No QR code generated for this order. Generate one first.");
        }

        // 4. Fetch QR code status from Razorpay
        QrCode qrCode;
        try {
            qrCode = razorpayClient.qrCode.fetch(qrCodeId);
        } catch (RazorpayException e) {
            log.error("Failed to fetch QR code status for order {}: {}",
                    orderId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch QR code status: " + e.getMessage());
        }

        // 5. Check if payment has been received
        String qrStatus = qrCode.get("status");
        int paymentsAmountReceived = qrCode.has("payments_amount_received")
                ? ((Number) qrCode.get("payments_amount_received")).intValue() : 0;
        int paymentAmount = qrCode.has("payment_amount")
                ? ((Number) qrCode.get("payment_amount")).intValue() : 0;

        if (paymentsAmountReceived >= paymentAmount && paymentAmount > 0) {
            // Payment received — update order
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMethod(PaymentMethod.ONLINE);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            log.info("QR payment confirmed for order {}: received={} paise",
                    orderId, paymentsAmountReceived);

            return Map.of(
                    "orderId", order.getOrderId().toString(),
                    "paymentStatus", "PAID",
                    "qrStatus", qrStatus,
                    "amountReceived", paymentsAmountReceived,
                    "message", "Payment received successfully."
            );
        }

        // Payment not yet received
        return Map.of(
                "orderId", order.getOrderId().toString(),
                "paymentStatus", order.getPaymentStatus().name(),
                "qrStatus", qrStatus,
                "amountReceived", paymentsAmountReceived,
                "message", "Payment not yet received. Please complete the UPI payment."
        );
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

