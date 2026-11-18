package com.api.batterymantra.service;

import com.api.batterymantra.config.RazorpayConfig;
import com.api.batterymantra.dto.payment.CheckoutItemSnapshot;
import com.api.batterymantra.dto.payment.CreateRazorpayOrderRequest;
import com.api.batterymantra.dto.payment.PaymentFailedRequest;
import com.api.batterymantra.dto.payment.PaymentVerificationResponse;
import com.api.batterymantra.dto.payment.QrCodeResponse;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.QrCode;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private static final int CHECKOUT_EXPIRY_MINUTES = 30;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final PendingPaymentCheckoutRepository pendingPaymentCheckoutRepository;
    private final ProductRepository productRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PincodeRepository pincodeRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final NotificationService notificationService;
    private final CampaignService campaignService;
    private final CouponService couponService;
    private final CentralInventoryService centralInventoryService;
    private final ObjectMapper objectMapper;

    /**
     * Initiates a Razorpay payment order WITHOUT creating a real row in `orders` or deducting stock.
     * Stores a checkout snapshot in `pending_payment_checkouts`. The real order is only created
     * once payment is verified (/verify) or captured (Razorpay webhook).
     */
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

        // 2. Validate stock availability (do NOT deduct stock until payment succeeds)
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

        // Resolve Target City for Dynamic Pricing
        UUID targetCityId = null;
        if (request.getSelectedCity() != null && !request.getSelectedCity().isBlank()) {
            var cityOpt = cityRepository.findByCityNameIgnoreCase(request.getSelectedCity().trim());
            if (cityOpt.isPresent()) {
                targetCityId = cityOpt.get().getCityId();
            }
        }
        if (targetCityId == null && address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            var pincodeOpt = pincodeRepository.findByCode(address.getPostalCode().trim());
            if (pincodeOpt.isPresent() && pincodeOpt.get().getCity() != null) {
                targetCityId = pincodeOpt.get().getCity().getCityId();
            }
        }
        if (targetCityId == null && address.getCity() != null && !address.getCity().isBlank()) {
            var cityOpt = cityRepository.findByCityNameIgnoreCase(address.getCity().trim());
            if (cityOpt.isPresent()) {
                targetCityId = cityOpt.get().getCityId();
            }
        }

        // 5. Build item snapshots and calculate total amount
        List<CheckoutItemSnapshot> itemSnapshots = new ArrayList<>();
        boolean shouldAutoAssign = false;
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal exchangeDiscount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItemList) {
            Product product = cartItem.getProduct();
            BigDecimal itemPrice = product.getProductPrice();
            BigDecimal unitExchangeDisc = product.getExchangeDiscount() != null
                    ? product.getExchangeDiscount()
                    : BigDecimal.ZERO;

            if (targetCityId != null && product.getCityPrices() != null) {
                final UUID finalCityId = targetCityId;
                var cpOpt = product.getCityPrices().stream()
                        .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(finalCityId))
                        .findFirst();
                if (cpOpt.isPresent()) {
                    if (cpOpt.get().getPrice() != null) {
                        itemPrice = cpOpt.get().getPrice();
                    }
                    if (cpOpt.get().getExchangeDiscount() != null
                            && cpOpt.get().getExchangeDiscount().compareTo(BigDecimal.ZERO) > 0) {
                        unitExchangeDisc = cpOpt.get().getExchangeDiscount();
                    }
                }
            }

            BigDecimal appliedItemExchangeDisc = cartItem.isExchangeOldBattery()
                    ? unitExchangeDisc
                    : BigDecimal.ZERO;

            itemSnapshots.add(CheckoutItemSnapshot.builder()
                    .productId(product.getProductId())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(itemPrice)
                    .exchangeOldBattery(cartItem.isExchangeOldBattery())
                    .exchangeDiscount(appliedItemExchangeDisc)
                    .build());

            subTotal = subTotal.add(itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            if (cartItem.isExchangeOldBattery()) {
                exchangeDiscount = exchangeDiscount.add(
                        unitExchangeDisc.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }

            if (product.isAutoAssignToPartner()) {
                shouldAutoAssign = true;
            }
        }

        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 5b. Evaluate Campaign offers and Coupons
        BigDecimal campaignDiscountAmount = BigDecimal.ZERO;
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        UUID appliedCampaignId = request.getAppliedCampaignId();
        String campaignName = null;
        String couponCode = request.getCouponCode() != null ? request.getCouponCode().trim() : null;

        var campaignValidation = campaignService.calculateCampaignDiscountForCart(
                cartItemList, targetCityId, couponCode, appliedCampaignId);

        if (campaignValidation.isApplicable() && campaignValidation.getDiscountAmount() != null
                && campaignValidation.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            campaignDiscountAmount = campaignValidation.getDiscountAmount();
            appliedCampaignId = campaignValidation.getCampaignId();
            campaignName = campaignValidation.getCampaignName();
        }

        // If couponCode was passed but not claimed by a campaign, check traditional CouponService
        if (couponCode != null && !couponCode.isBlank() && appliedCampaignId == null) {
            List<com.api.batterymantra.dto.coupon.ApplyCouponRequest.ApplyCouponCartItemDto> couponCartItems =
                    cartItemList.stream().map(ci -> com.api.batterymantra.dto.coupon.ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                            .productId(ci.getProduct() != null && ci.getProduct().getProductId() != null ? ci.getProduct().getProductId().toString() : null)
                            .categoryId(ci.getProduct() != null && ci.getProduct().getProductCategory() != null && ci.getProduct().getProductCategory().getCategoryId() != null ? ci.getProduct().getProductCategory().getCategoryId().toString() : null)
                            .brandId(ci.getProduct() != null && ci.getProduct().getBrand() != null && ci.getProduct().getBrand().getBrandId() != null ? ci.getProduct().getBrand().getBrandId().toString() : null)
                            .productPrice(ci.getProduct() != null && ci.getProduct().getProductPrice() != null ? ci.getProduct().getProductPrice().doubleValue() : 0.0)
                            .quantity(ci.getQuantity())
                            .build()).toList();

            var couponResponse = couponService.applyCoupon(com.api.batterymantra.dto.coupon.ApplyCouponRequest.builder()
                    .code(couponCode)
                    .cartTotal(total.subtract(campaignDiscountAmount).doubleValue())
                    .cartItems(couponCartItems)
                    .build());
            if (Boolean.TRUE.equals(couponResponse.getIsValid())) {
                couponDiscountAmount = BigDecimal.valueOf(couponResponse.getDiscountAmount());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, couponResponse.getMessage());
            }
        }

        BigDecimal totalDiscount = campaignDiscountAmount.add(couponDiscountAmount);
        total = total.subtract(totalDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 6. Resolve auto-assign partner (persisted in pending checkout, applied when order is created)
        PartnerProfile matchedPartner = null;
        if (shouldAutoAssign) {
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
        }

        // 7. Create Razorpay order
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

        // 8. Persist PendingPaymentCheckout snapshot ONLY — NO row in `orders`, NO stock deduction
        String itemsSnapshotJson;
        try {
            itemsSnapshotJson = objectMapper.writeValueAsString(itemSnapshots);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize checkout items snapshot: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to prepare payment checkout session");
        }

        PendingPaymentCheckout pendingCheckout = PendingPaymentCheckout.builder()
                .razorpayOrderId(razorpayOrderId)
                .customer(cart.getCustomer())
                .shippingAddress(address)
                .deliveryMethod(deliveryMethod)
                .installationDate(request.getInstallationDate())
                .totalAmount(total)
                .exchangeDiscount(exchangeDiscount)
                .selectedCity(request.getSelectedCity())
                .appliedCampaignId(appliedCampaignId)
                .campaignName(campaignName)
                .campaignDiscountAmount(campaignDiscountAmount)
                .discountAmount(totalDiscount)
                .couponCode(couponCode)
                .assignedPartner(matchedPartner)
                .itemsSnapshotJson(itemsSnapshotJson)
                .status(PaymentCheckoutStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(CHECKOUT_EXPIRY_MINUTES))
                .build();

        pendingPaymentCheckoutRepository.save(pendingCheckout);

        log.info("Razorpay payment intent created: {} (pending checkout id: {}) — order deferred until payment succeeds",
                razorpayOrderId, pendingCheckout.getId());

        return new RazorpayOrderResponse(
                razorpayOrderId,
                amountInPaise,
                "INR",
                razorpayConfig.getKeyId(),
                null
        );
    }

    /**
     * Verifies Razorpay HMAC-SHA256 signature and creates the real order (idempotently).
     * If signature is invalid, no order is created.
     */
    @Transactional
    public PaymentVerificationResponse verifyPayment(UUID customerId,
                                                     VerifyPaymentRequest request) {
        // 1. Verify HMAC-SHA256 signature first (constant-time)
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        if (!verifySignature(payload, razorpayConfig.getKeySecret(), request.getRazorpaySignature())) {
            log.warn("Payment verification failed (invalid signature) for Razorpay order ID: {}",
                    request.getRazorpayOrderId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment verification failed. Invalid signature.");
        }

        // 2. Look up PendingPaymentCheckout with pessimistic write lock (prevents race with webhook)
        Optional<PendingPaymentCheckout> checkoutOpt =
                pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(request.getRazorpayOrderId());

        if (checkoutOpt.isPresent()) {
            PendingPaymentCheckout checkout = checkoutOpt.get();

            if (!checkout.getCustomer().getUserId().equals(customerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not authorized to verify this payment");
            }

            Orders confirmedOrder = finalizePaidCheckout(checkout, request.getRazorpayPaymentId(), "CUSTOMER");

            return new PaymentVerificationResponse(
                    confirmedOrder.getOrderId(),
                    confirmedOrder.getOrderStatus().name(),
                    confirmedOrder.getPaymentStatus().name(),
                    "Payment verified successfully"
            );
        }

        // 3. Legacy fallback for any in-flight order created before deferred order creation rollout
        Orders legacyOrder = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment checkout session not found for Razorpay order ID: "
                                + request.getRazorpayOrderId()));

        if (!legacyOrder.getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to verify this payment");
        }

        Orders confirmedLegacy = confirmLegacyOrder(legacyOrder);

        return new PaymentVerificationResponse(
                confirmedLegacy.getOrderId(),
                confirmedLegacy.getOrderStatus().name(),
                confirmedLegacy.getPaymentStatus().name(),
                "Payment verified successfully"
        );
    }

    /**
     * Records a payment failure / customer dismissal reported by the frontend.
     * No order exists and no stock was deducted; simply marks the pending checkout as FAILED
     * unless it has already been COMPLETED.
     */
    @Transactional
    public Map<String, Object> markPaymentFailed(UUID customerId, PaymentFailedRequest request) {
        PendingPaymentCheckout checkout = pendingPaymentCheckoutRepository
                .findByRazorpayOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment checkout session not found for Razorpay order ID: "
                                + request.getRazorpayOrderId()));

        if (!checkout.getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to update this payment session");
        }

        if (checkout.getStatus() == PaymentCheckoutStatus.COMPLETED) {
            return Map.of(
                    "razorpayOrderId", checkout.getRazorpayOrderId(),
                    "status", PaymentCheckoutStatus.COMPLETED.name(),
                    "orderId", checkout.getCreatedOrderId() != null ? checkout.getCreatedOrderId().toString() : "",
                    "message", "Payment was already completed."
            );
        }

        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? truncate(request.getReason().trim(), MAX_FAILURE_REASON_LENGTH)
                : "Payment failed or cancelled by customer";

        checkout.setStatus(PaymentCheckoutStatus.FAILED);
        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().isBlank()) {
            checkout.setRazorpayPaymentId(request.getRazorpayPaymentId().trim());
        }
        checkout.setFailureReason(reason);
        pendingPaymentCheckoutRepository.save(checkout);

        log.info("Marked pending payment checkout {} (rzpOrder={}) as FAILED. Reason: {}. No order created.",
                checkout.getId(), checkout.getRazorpayOrderId(), reason);

        return Map.of(
                "razorpayOrderId", checkout.getRazorpayOrderId(),
                "status", PaymentCheckoutStatus.FAILED.name(),
                "message", "Payment failure recorded. No order was created."
        );
    }

    /**
     * Handles server-to-server Razorpay webhooks (payment.captured, order.paid, payment.failed).
     * Verifies X-Razorpay-Signature against razorpay.webhook-secret before processing.
     */
    @Transactional
    public Map<String, Object> handleWebhook(String rawPayload, String signature) {
        String webhookSecret = razorpayConfig.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Razorpay webhook received but razorpay.webhook-secret is not configured!");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Webhook secret is not configured.");
        }

        if (!verifySignature(rawPayload, webhookSecret, signature)) {
            log.warn("Rejected Razorpay webhook due to invalid X-Razorpay-Signature.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid webhook signature.");
        }

        JSONObject root;
        try {
            root = new JSONObject(rawPayload);
        } catch (JSONException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook JSON payload.");
        }

        String event = root.optString("event", "");
        JSONObject payload = root.optJSONObject("payload");

        if ("payment.captured".equals(event) || "order.paid".equals(event)) {
            JSONObject paymentEntity = extractEntity(payload, "payment");
            JSONObject orderEntity = extractEntity(payload, "order");

            String razorpayOrderId = paymentEntity != null ? paymentEntity.optString("order_id", "") : "";
            if (razorpayOrderId.isBlank() && orderEntity != null) {
                razorpayOrderId = orderEntity.optString("id", "");
            }
            String razorpayPaymentId = paymentEntity != null ? paymentEntity.optString("id", "") : "";

            if (razorpayOrderId.isBlank()) {
                log.info("Ignoring Razorpay webhook event '{}' without order_id (paymentId={})", event, razorpayPaymentId);
                return Map.of("status", "ignored", "event", event);
            }

            Optional<PendingPaymentCheckout> checkoutOpt =
                    pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId);

            if (checkoutOpt.isPresent()) {
                Orders confirmedOrder = finalizePaidCheckout(checkoutOpt.get(), razorpayPaymentId, "RAZORPAY_WEBHOOK");
                return Map.of(
                        "status", "processed",
                        "event", event,
                        "orderId", confirmedOrder.getOrderId().toString()
                );
            }

            Optional<Orders> legacyOrderOpt = orderRepository.findByRazorpayOrderId(razorpayOrderId);
            if (legacyOrderOpt.isPresent()) {
                Orders confirmedLegacy = confirmLegacyOrder(legacyOrderOpt.get());
                return Map.of(
                        "status", "processed",
                        "event", event,
                        "orderId", confirmedLegacy.getOrderId().toString()
                );
            }

            log.warn("Razorpay webhook '{}' received for unknown razorpayOrderId={}", event, razorpayOrderId);
            return Map.of("status", "ignored", "event", event, "message", "No matching checkout found");
        }

        if ("payment.failed".equals(event)) {
            JSONObject paymentEntity = extractEntity(payload, "payment");
            String razorpayOrderId = paymentEntity != null ? paymentEntity.optString("order_id", "") : "";
            String razorpayPaymentId = paymentEntity != null ? paymentEntity.optString("id", "") : "";

            if (razorpayOrderId.isBlank()) {
                return Map.of("status", "ignored", "event", event);
            }

            String reason = extractFailureReason(paymentEntity);

            Optional<PendingPaymentCheckout> checkoutOpt =
                    pendingPaymentCheckoutRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId);

            if (checkoutOpt.isPresent()) {
                PendingPaymentCheckout checkout = checkoutOpt.get();
                if (checkout.getStatus() == PaymentCheckoutStatus.PENDING) {
                    checkout.setStatus(PaymentCheckoutStatus.FAILED);
                    if (!razorpayPaymentId.isBlank()) {
                        checkout.setRazorpayPaymentId(razorpayPaymentId);
                    }
                    checkout.setFailureReason(reason);
                    pendingPaymentCheckoutRepository.save(checkout);
                    log.info("Webhook payment.failed recorded for rzpOrder={}: {}. No order created.",
                            razorpayOrderId, reason);
                } else {
                    log.info("Webhook payment.failed ignored for rzpOrder={} because checkout status is already {}",
                            razorpayOrderId, checkout.getStatus());
                }
            }

            return Map.of("status", "recorded", "event", event);
        }

        log.debug("Ignoring unhandled Razorpay webhook event: {}", event);
        return Map.of("status", "ignored", "event", event);
    }

    /**
     * Idempotently converts a verified/captured PendingPaymentCheckout into a real CONFIRMED/PAID order.
     * Safe to invoke concurrently from both /verify and Razorpay webhook (caller holds pessimistic lock).
     */
    private Orders finalizePaidCheckout(PendingPaymentCheckout checkout,
                                        String razorpayPaymentId,
                                        String performedBy) {
        // Idempotency guard #1: already finalized via createdOrderId
        if (checkout.getStatus() == PaymentCheckoutStatus.COMPLETED && checkout.getCreatedOrderId() != null) {
            Optional<Orders> existing = orderRepository.findById(checkout.getCreatedOrderId());
            if (existing.isPresent()) {
                log.info("Checkout {} already finalized to order {}, returning idempotently.",
                        checkout.getId(), existing.get().getOrderId());
                return existing.get();
            }
        }

        // Idempotency guard #2: order already exists by razorpayOrderId
        Optional<Orders> existingByRzp = orderRepository.findByRazorpayOrderId(checkout.getRazorpayOrderId());
        if (existingByRzp.isPresent()) {
            Orders existingOrder = existingByRzp.get();
            checkout.setStatus(PaymentCheckoutStatus.COMPLETED);
            checkout.setCreatedOrderId(existingOrder.getOrderId());
            if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
                checkout.setRazorpayPaymentId(razorpayPaymentId);
            }
            pendingPaymentCheckoutRepository.save(checkout);
            log.info("Order {} already existed for rzpOrder={}, reconciled checkout {}.",
                    existingOrder.getOrderId(), checkout.getRazorpayOrderId(), checkout.getId());
            return existingOrder;
        }

        // Reconstruct order items from the persisted checkout snapshot
        List<CheckoutItemSnapshot> snapshots = deserializeItemSnapshots(checkout.getItemsSnapshotJson());
        List<OrderItems> orderItems = new ArrayList<>();

        Orders order = Orders.builder()
                .customer(checkout.getCustomer())
                .shippingAddress(checkout.getShippingAddress())
                .paymentStatus(PaymentStatus.PAID)
                .orderStatus(OrderStatus.CONFIRMED)
                .deliveryMethod(checkout.getDeliveryMethod())
                .paymentMethod(PaymentMethod.ONLINE)
                .installationDate(checkout.getInstallationDate())
                .razorpayOrderId(checkout.getRazorpayOrderId())
                .totalAmount(checkout.getTotalAmount())
                .exchangeDiscount(checkout.getExchangeDiscount())
                .selectedCity(checkout.getSelectedCity())
                .appliedCampaignId(checkout.getAppliedCampaignId())
                .campaignName(checkout.getCampaignName())
                .campaignDiscountAmount(checkout.getCampaignDiscountAmount())
                .discountAmount(checkout.getDiscountAmount())
                .couponCode(checkout.getCouponCode())
                .assignedPartner(checkout.getAssignedPartner())
                .build();

        for (CheckoutItemSnapshot snap : snapshots) {
            Product product = productRepository.findById(snap.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Product not found while finalizing paid order: " + snap.getProductId()));
            OrderItems item = OrderItems.builder()
                    .order(order)
                    .product(product)
                    .quantity(snap.getQuantity())
                    .priceAtPurchase(snap.getPriceAtPurchase())
                    .exchangeOldBattery(snap.isExchangeOldBattery())
                    .exchangeDiscount(snap.getExchangeDiscount())
                    .build();
            orderItems.add(item);
        }
        order.setOrderItems(orderItems);

        Orders savedOrder = orderRepository.save(order);

        // Concurrency-safe stock deduction on payment success (never rolls back a paid order on shortage)
        boolean stockDeducted = centralInventoryService.deductStockForPaidOrder(savedOrder, performedBy);
        if (!stockDeducted) {
            String shortageNote = "[STOCK SHORTAGE ALERT] Online payment captured"
                    + (razorpayPaymentId != null && !razorpayPaymentId.isBlank() ? " (" + razorpayPaymentId + ")" : "")
                    + ", but stock was insufficient at payment confirmation. Admin resolution required.";
            savedOrder.setEngineerNotes(shortageNote);
            savedOrder = orderRepository.save(savedOrder);
        }

        // Mark checkout COMPLETED
        checkout.setStatus(PaymentCheckoutStatus.COMPLETED);
        checkout.setCreatedOrderId(savedOrder.getOrderId());
        if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
            checkout.setRazorpayPaymentId(razorpayPaymentId);
        }
        checkout.setFailureReason(null);
        pendingPaymentCheckoutRepository.save(checkout);

        // Clear the user's cart
        UUID customerId = checkout.getCustomer().getUserId();
        Cart cart = cartRepository.findByUserId(customerId).orElse(null);
        if (cart != null) {
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }

        // Increment coupon usage if traditional coupon was used
        if (savedOrder.getCouponCode() != null && !savedOrder.getCouponCode().isBlank()
                && savedOrder.getDiscountAmount() != null && savedOrder.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && savedOrder.getAppliedCampaignId() == null) {
            couponService.incrementCouponUsage(savedOrder.getCouponCode());
        }

        // Send customer SMS notification
        String customerPhone = savedOrder.getCustomer().getPhoneNumber();
        String customerName = savedOrder.getCustomer().getUsername();
        String orderIdStr = savedOrder.getOrderId().toString();

        if (customerPhone != null && !customerPhone.isBlank()) {
            smsService.sendOrderPlacedSms(
                    customerPhone,
                    customerName,
                    orderIdStr,
                    String.valueOf(savedOrder.getTotalAmount()),
                    savedOrder.getPlacedAt() != null ? savedOrder.getPlacedAt().toString() : "",
                    "Your Product",
                    "Online"
            );
        }

        // Notify Admins
        notifyAdminsOnOnlineOrder(savedOrder, stockDeducted);

        log.info("Paid online order {} created & confirmed via {} (rzpOrder={}, rzpPayment={}, stockDeducted={})",
                savedOrder.getOrderId(), performedBy, checkout.getRazorpayOrderId(), razorpayPaymentId, stockDeducted);

        return savedOrder;
    }

    private Orders confirmLegacyOrder(Orders order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID && order.getOrderStatus() == OrderStatus.CONFIRMED) {
            return order;
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);
        Orders savedOrder = orderRepository.save(order);

        UUID customerId = savedOrder.getCustomer().getUserId();
        Cart cart = cartRepository.findByUserId(customerId).orElse(null);
        if (cart != null) {
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }

        if (savedOrder.getCouponCode() != null && !savedOrder.getCouponCode().isBlank()
                && savedOrder.getDiscountAmount() != null && savedOrder.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && savedOrder.getAppliedCampaignId() == null) {
            couponService.incrementCouponUsage(savedOrder.getCouponCode());
        }

        String customerPhone = savedOrder.getCustomer().getPhoneNumber();
        String customerName = savedOrder.getCustomer().getUsername();
        String orderIdStr = savedOrder.getOrderId().toString();

        if (customerPhone != null && !customerPhone.isBlank()) {
            smsService.sendOrderPlacedSms(
                    customerPhone,
                    customerName,
                    orderIdStr,
                    String.valueOf(savedOrder.getTotalAmount()),
                    savedOrder.getPlacedAt() != null ? savedOrder.getPlacedAt().toString() : "",
                    "Your Product",
                    "Online"
            );
        }

        log.info("Legacy online order verified & confirmed: {}", savedOrder.getOrderId());
        return savedOrder;
    }

    private void notifyAdminsOnOnlineOrder(Orders order, boolean stockDeducted) {
        try {
            List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
            if (admins == null || admins.isEmpty()) {
                return;
            }
            String orderIdStr = order.getOrderId().toString();
            String shortId = orderIdStr.length() > 8 ? orderIdStr.substring(0, 8) : orderIdStr;
            String title = stockDeducted ? "New Online Order Paid" : "URGENT: Paid Order Stock Shortage";
            String message = stockDeducted
                    ? "Online Order #" + shortId + "... has been paid and confirmed."
                    : "Online Order #" + shortId + "... was PAID but stock was insufficient! Please resolve immediately.";

            for (User admin : admins) {
                notificationService.sendPushNotification(admin.getUserId(), title, message, null);
            }
        } catch (Exception e) {
            log.warn("Failed to send admin notification for online order {}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private List<CheckoutItemSnapshot> deserializeItemSnapshots(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CheckoutItemSnapshot>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize checkout item snapshots: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Corrupted checkout snapshot data");
        }
    }

    private JSONObject extractEntity(JSONObject payload, String key) {
        if (payload == null) {
            return null;
        }
        JSONObject wrapper = payload.optJSONObject(key);
        return wrapper != null ? wrapper.optJSONObject("entity") : null;
    }

    private String extractFailureReason(JSONObject paymentEntity) {
        if (paymentEntity == null) {
            return "Payment failed";
        }
        String description = paymentEntity.optString("error_description", "").trim();
        String reason = paymentEntity.optString("error_reason", "").trim();
        String source = paymentEntity.optString("error_source", "").trim();

        if (!description.isBlank()) {
            return truncate(description, MAX_FAILURE_REASON_LENGTH);
        }
        if (!source.isBlank() && !reason.isBlank()) {
            return truncate(source + " - " + reason, MAX_FAILURE_REASON_LENGTH);
        }
        if (!reason.isBlank()) {
            return truncate(reason, MAX_FAILURE_REASON_LENGTH);
        }
        return "Payment failed";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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

    private boolean verifySignature(String data, String secret, String actualSignature) {
        if (data == null || secret == null || secret.isBlank()
                || actualSignature == null || actualSignature.isBlank()) {
            return false;
        }
        String expectedSignature = calculateHmacSha256(data, secret);
        if (expectedSignature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                actualSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

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
