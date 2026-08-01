package com.api.batterymantra.service;

import com.api.batterymantra.entity.enums.PaymentMethod;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.repository.PincodeRepository;

import com.api.batterymantra.dto.order.AdminCreateOrderRequest;
import com.api.batterymantra.dto.order.AdminOrderItemRequest;
import com.api.batterymantra.dto.order.CheckoutRequest;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentStatus;
import com.api.batterymantra.repository.AddressRepository;
import com.api.batterymantra.repository.CartRepository;
import com.api.batterymantra.repository.OrderRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.repository.PartnerProfileRepository;
import com.api.batterymantra.util.OrderMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final EngineerProfileRepository engineerProfileRepository;
    private final PincodeRepository pincodeRepository;
    private final OrderMapper orderMapper;
    private final SmsService smsService;
    private final CouponService couponService;

    @Transactional
    public OrderResponse placeOrder(UUID customerId, CheckoutRequest request) {
        // Fetch the cart for the customer
        Cart cart = cartRepository.findByUserId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cart not found for user: " + customerId));

        List<CartItem> cartItemList = getCartItems(customerId, cart);

        // Fetch the Address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Address not found for user: " + customerId));

        if (address.getIsDeleted() != null && address.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot use a deleted address for checkout");
        }

        if (!address.getUser().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use a valid address");
        }

        DeliveryMethod deliveryMethod = null;
        try {
            deliveryMethod = DeliveryMethod.valueOf(request.getDeliveryMethod());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or missing Delivery Method");
        }

        PaymentMethod paymentMethod = null;
        try {
            if (request.getPaymentMethod() != null) {
                paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
            } else {
                paymentMethod = PaymentMethod.COD; // Default to COD
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Payment Method");
        }

        // Creating a new Order
        Orders orders = Orders.builder()
                .customer(cart.getCustomer())
                .shippingAddress(address)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .deliveryMethod(deliveryMethod)
                .paymentMethod(paymentMethod)
                .installationDate(request.getInstallationDate())
                .build();

        // Converting Cart Items to Order Items
        List<OrderItems> orderItems = new ArrayList<>();
        boolean shouldAutoAssign = false;

        for (CartItem cartItem : cartItemList) {
            OrderItems items = OrderItems.builder()
                    .order(orders)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getProduct().getProductPrice())
                    .build();

            if (cartItem.getProduct().isAutoAssignToPartner()) {
                shouldAutoAssign = true;
            }

            orderItems.add(items);
        }

        // Robust Logic for Auto-assignment to Partner (by Pincode or City)
        if (shouldAutoAssign && address != null) {
            PartnerProfile matchedPartner = null;

            // 1. Try matching by Pincode / Postal Code
            if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
                String cleanPincode = address.getPostalCode().trim();
                var pincodeOpt = pincodeRepository.findByCode(cleanPincode);
                if (pincodeOpt.isPresent() && pincodeOpt.get().getCity() != null) {
                    UUID cityId = pincodeOpt.get().getCity().getCityId();
                    matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityId(cityId)
                            .orElse(null);
                }
            }

            // 2. Fallback: Try matching by Shipping Address City Name
            if (matchedPartner == null && address.getCity() != null && !address.getCity().isBlank()) {
                String cleanCity = address.getCity().trim();
                matchedPartner = partnerProfileRepository
                        .findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(cleanCity).orElse(null);
            }

            if (matchedPartner != null) {
                orders.setAssignedPartner(matchedPartner);
            }
        }

        // Calculating the Total Amount
        BigDecimal subTotal = orderItems.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Exchange Discount
        BigDecimal exchangeDiscount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItemList) {
            if (cartItem.isExchangeOldBattery() && cartItem.getProduct().getExchangeDiscount() != null) {
                exchangeDiscount = exchangeDiscount.add(cartItem.getProduct().getExchangeDiscount()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }

        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // Apply coupon if present
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            var couponResponse = couponService.applyCoupon(request.getCouponCode(), total.doubleValue());
            if (couponResponse.getIsValid()) {
                discountAmount = BigDecimal.valueOf(couponResponse.getDiscountAmount());
                total = total.subtract(discountAmount);
                if (total.compareTo(BigDecimal.ZERO) < 0) {
                    total = BigDecimal.ZERO;
                }
                orders.setCouponCode(request.getCouponCode());
                orders.setDiscountAmount(discountAmount);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, couponResponse.getMessage());
            }
        }

        orders.setOrderItems(orderItems);
        orders.setTotalAmount(total);
        orders.setExchangeDiscount(exchangeDiscount);

        // Save the Order
        Orders placedOrder = orderRepository.save(orders);
        
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank() && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            couponService.incrementCouponUsage(request.getCouponCode());
        }

        if (paymentMethod != PaymentMethod.ONLINE) {
            // Clearing Cart
            cart.getCartItems().clear();
            cartRepository.save(cart);

            // Send SMS
            String customerPhone = cart.getCustomer().getPhoneNumber();
            String customerName = cart.getCustomer().getUsername();
            String orderIdStr = placedOrder.getOrderId().toString();

            if (customerPhone != null && !customerPhone.isBlank()) {
                smsService.sendOrderPlacedSms(customerPhone, customerName, orderIdStr, String.valueOf(placedOrder.getTotalAmount()), placedOrder.getPlacedAt() != null ? placedOrder.getPlacedAt().toString() : "", "Your Product", "Online");
            }
            // admin alert handled inside sendOrderPlacedSms
        }

        return orderMapper.toOrderResponse(placedOrder);
    }

    @Transactional
    public OrderResponse createAdminOrder(AdminCreateOrderRequest request) {
        // Fetch User
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer not found: " + request.getCustomerId()));

        // Fetch Address
        Address address;
        if (request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        } else {
            List<Address> addresses = addressRepository.findAllByUserUserIdAndIsDeletedFalse(customer.getUserId());
            if (addresses.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Customer has no addresses to use as default");
            }
            address = addresses.get(0);
        }

        DeliveryMethod deliveryMethod;
        try {
            deliveryMethod = DeliveryMethod.valueOf(request.getDeliveryMethod());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Delivery Method");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (Exception e) {
            paymentMethod = PaymentMethod.COD;
        }

        Orders orders = Orders.builder()
                .customer(customer)
                .shippingAddress(address)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .deliveryMethod(deliveryMethod)
                .paymentMethod(paymentMethod)
                .installationDate(request.getInstallationDate())
                .build();

        List<OrderItems> orderItems = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal exchangeDiscount = BigDecimal.ZERO;

        for (AdminOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: " + itemReq.getProductId()));

            OrderItems item = OrderItems.builder()
                    .order(orders)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .priceAtPurchase(product.getProductPrice())
                    .build();

            orderItems.add(item);
            subTotal = subTotal.add(product.getProductPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            if (itemReq.isExchangeOldBattery() && product.getExchangeDiscount() != null) {
                exchangeDiscount = exchangeDiscount
                        .add(product.getExchangeDiscount().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }

        // Auto assign to partner
        if (address != null) {
            PartnerProfile matchedPartner = null;
            if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
                String cleanPincode = address.getPostalCode().trim();
                var pincodeOpt = pincodeRepository.findByCode(cleanPincode);
                if (pincodeOpt.isPresent() && pincodeOpt.get().getCity() != null) {
                    UUID cityId = pincodeOpt.get().getCity().getCityId();
                    matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityId(cityId)
                            .orElse(null);
                }
            }
            if (matchedPartner == null && address.getCity() != null && !address.getCity().isBlank()) {
                String cleanCity = address.getCity().trim();
                matchedPartner = partnerProfileRepository
                        .findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(cleanCity).orElse(null);
            }
            if (matchedPartner != null) {
                orders.setAssignedPartner(matchedPartner);
            }
        }

        if (request.getDiscount() != null) {
            exchangeDiscount = exchangeDiscount.add(request.getDiscount());
        }

        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        orders.setOrderItems(orderItems);
        orders.setTotalAmount(total);
        orders.setExchangeDiscount(exchangeDiscount);

        Orders placedOrder = orderRepository.save(orders);

        // Send SMS
        String customerPhone = customer.getPhoneNumber();
        String customerName = customer.getUsername();
        String orderIdStr = placedOrder.getOrderId().toString();

        if (customerPhone != null && !customerPhone.isBlank()) {
            smsService.sendOrderPlacedSms(customerPhone, customerName, orderIdStr, String.valueOf(placedOrder.getTotalAmount()), placedOrder.getPlacedAt() != null ? placedOrder.getPlacedAt().toString() : "", "Your Product", "Online");
        }
        // admin alert handled inside sendOrderPlacedSms

        return orderMapper.toOrderResponse(placedOrder);
    }

    // To Get All the Orders Placed by a Customer
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID customerId) {
        List<Orders> orders = orderRepository.findByCustomer_UserIdOrderByPlacedAtDesc(customerId).stream()
                .filter(o -> !(o.getPaymentMethod() == PaymentMethod.ONLINE && o.getPaymentStatus() != PaymentStatus.PAID))
                .toList();

        if (orders.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No orders found for user: " + customerId);

        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }

    // To Get a Particular Order by ID
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID customerId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order");
        }

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID customerId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending orders can be cancelled");
        }

        // Restore stock
        for (OrderItems item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setProductStock(product.getProductStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Orders cancelledOrder = orderRepository.save(order);

        // Send SMS
        String customerPhone = order.getCustomer().getPhoneNumber();
        String customerName = order.getCustomer().getUsername();
        String orderIdStr = order.getOrderId().toString();
        if (customerPhone != null && !customerPhone.isBlank()) {
            smsService.sendOrderCancelledSms(customerPhone, customerName, "Your Product", orderIdStr, "N/A");
        }

        return orderMapper.toOrderResponse(cancelledOrder);
    }

    // ===== ADMIN Methods =====

    /**
     * Get all orders in the system (ADMIN only)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> !(o.getPaymentMethod() == PaymentMethod.ONLINE && o.getPaymentStatus() != PaymentStatus.PAID))
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    /**
     * Update order status (ADMIN only)
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (order.getAssignedPartner() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Order is assigned to partner '" + order.getAssignedPartner().getBusinessName()
                            + "'. Status updates must be managed by the assigned partner.");
        }

        // Validate status transition
        validateStatusTransition(order, newStatus);

        // If cancelling, restore stock
        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            for (OrderItems item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setProductStock(product.getProductStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        // Handle SHIPPED / OUT_FOR_DELIVERY status to generate security code
        if ((newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY) && order.getDeliverySecurityCode() == null) {
            String securityCode = String.valueOf(1000 + new Random().nextInt(9000));
            order.setDeliverySecurityCode(securityCode);
        }

        order.setOrderStatus(newStatus);
        Orders updatedOrder = orderRepository.save(order);

        // Send SMS
        String customerPhone = order.getCustomer().getPhoneNumber();
        String customerName = order.getCustomer().getUsername();
        String orderIdStr = order.getOrderId().toString();

        if (customerPhone != null && !customerPhone.isBlank()) {
            if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY) {
                String engName = order.getAssignedEngineer() != null
                        ? order.getAssignedEngineer().getFirstName() + " " + order.getAssignedEngineer().getLastName()
                        : "Engineer";
                String engPhone = order.getAssignedEngineer() != null
                        ? order.getAssignedEngineer().getUser().getPhoneNumber()
                        : "N/A";
                smsService.sendOrderDispatchedSms(customerPhone, customerName, "Your Product", orderIdStr, engName, engPhone, updatedOrder.getDeliverySecurityCode());
            } else if (newStatus == OrderStatus.DELIVERED) {
                smsService.sendOrderDeliveredSms(customerPhone, customerName, "Your Product", orderIdStr);
            } else if (newStatus == OrderStatus.CANCELLED) {
                smsService.sendOrderCancelledSms(customerPhone, customerName, "Your Product", orderIdStr, "N/A");
            }
        }

        return orderMapper.toOrderResponse(updatedOrder);
    }

    private void validateStatusTransition(Orders order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status of a delivered order");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status of a cancelled order");
        }

        // 1. Prevent processing or dispatching unpaid online orders
        if (order.getPaymentMethod() == PaymentMethod.ONLINE
                && order.getPaymentStatus() != PaymentStatus.PAID
                && newStatus != OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot process or dispatch an Online Payment order whose payment is pending or failed! The customer has not completed the payment.");
        }

        // 2. Prevent dispatching or delivering without an assigned engineer
        if ((newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY
                || newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.INSTALLED)
                && order.getAssignedEngineer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot change order status to " + newStatus + " without assigning a Field Engineer! Please assign a technician first.");
        }
    }

    // ===== PARTNER Methods =====

    @Transactional(readOnly = true)
    public List<OrderResponse> getPartnerOrders(UUID partnerProfileId) {
        List<Orders> orders = orderRepository.findByAssignedPartner_IdOrderByPlacedAtDesc(partnerProfileId).stream()
                .filter(o -> !(o.getPaymentMethod() == PaymentMethod.ONLINE && o.getPaymentStatus() != PaymentStatus.PAID))
                .toList();
        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse updatePartnerOrderStatus(UUID orderId, OrderStatus newStatus, UUID partnerProfileId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (order.getAssignedPartner() == null || !order.getAssignedPartner().getId().equals(partnerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied to this order. It is not assigned to you.");
        }

        validateStatusTransition(order, newStatus);

        // Handle SHIPPED / OUT_FOR_DELIVERY status to generate security code
        if ((newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY) && order.getDeliverySecurityCode() == null) {
            String securityCode = String.valueOf(1000 + new Random().nextInt(9000));
            order.setDeliverySecurityCode(securityCode);
        }

        order.setOrderStatus(newStatus);
        Orders updatedOrder = orderRepository.save(order);

        // Send SMS
        String customerPhone = order.getCustomer().getPhoneNumber();
        String customerName = order.getCustomer().getUsername();
        String orderIdStr = order.getOrderId().toString();

        if (customerPhone != null && !customerPhone.isBlank()) {
            if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY) {
                String engName = order.getAssignedEngineer() != null
                        ? order.getAssignedEngineer().getFirstName() + " " + order.getAssignedEngineer().getLastName()
                        : "Engineer";
                String engPhone = order.getAssignedEngineer() != null
                        ? order.getAssignedEngineer().getUser().getPhoneNumber()
                        : "N/A";
                smsService.sendOrderDispatchedSms(customerPhone, customerName, "Your Product", orderIdStr, engName, engPhone, updatedOrder.getDeliverySecurityCode());
            } else if (newStatus == OrderStatus.DELIVERED) {
                smsService.sendOrderDeliveredSms(customerPhone, customerName, "Your Product", orderIdStr);
            } else if (newStatus == OrderStatus.CANCELLED) {
                smsService.sendOrderCancelledSms(customerPhone, customerName, "Your Product", orderIdStr, "N/A");
            }
        }

        return orderMapper.toOrderResponse(updatedOrder);
    }

    private List<CartItem> getCartItems(UUID customerId, Cart cart) {
        List<CartItem> cartItemList = cart.getCartItems();

        // Check if cart is empty or not
        if (cartItemList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty for user: " + customerId);
        }

        // Checking Stock Availability
        for (CartItem cartItem : cartItemList) {
            Product product = cartItem.getProduct();
            if (product.getProductStock() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product: " + product.getProductName());
            } else {
                // Reduce the stock
                product.setProductStock(product.getProductStock() - cartItem.getQuantity());
                productRepository.save(product);
            }
        }
        return cartItemList;
    }

    @Transactional
    public OrderResponse assignPartner(UUID orderId, UUID partnerId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner not found"));

        order.setAssignedPartner(partner);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse assignEngineerByAdmin(UUID orderId, UUID engineerId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getAssignedPartner() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Order is assigned to partner '" + order.getAssignedPartner().getBusinessName()
                            + "'. Engineers must be assigned by the partner branch.");
        }

        EngineerProfile engineer = engineerProfileRepository.findById(engineerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found"));

        if (engineer.getPartnerProfile() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin can only assign Direct Admin engineers.");
        }

        order.setAssignedEngineer(engineer);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse assignEngineerByPartner(UUID orderId, UUID engineerId, UUID partnerProfileId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getAssignedPartner() == null || !order.getAssignedPartner().getId().equals(partnerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. Order is not assigned to your partner branch.");
        }

        EngineerProfile engineer = engineerProfileRepository.findById(engineerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found"));

        if (engineer.getPartnerProfile() == null || !engineer.getPartnerProfile().getId().equals(partnerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only assign engineers belonging to your partner branch.");
        }

        order.setAssignedEngineer(engineer);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }
}
