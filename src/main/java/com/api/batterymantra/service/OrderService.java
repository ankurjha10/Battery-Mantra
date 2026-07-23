package com.api.batterymantra.service;

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

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final com.api.batterymantra.repository.EngineerProfileRepository engineerProfileRepository;
    private final com.api.batterymantra.repository.PincodeRepository pincodeRepository;
    private final OrderMapper orderMapper;


    @Transactional
    public OrderResponse placeOrder(UUID customerId, CheckoutRequest request) {
        // Fetch the cart for the customer
        Cart cart = cartRepository.findByUserId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cart not found for user: " + customerId));

        List<CartItem> cartItemList = getCartItems(customerId, cart);

        //Fetch the Address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Address not found for user: " + customerId));

        if (address.getIsDeleted() != null && address.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot use a deleted address for checkout");
        }

        if (!address.getUser().getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use a valid address");
        }

        com.api.batterymantra.entity.enums.DeliveryMethod deliveryMethod = null;
        try {
            deliveryMethod = com.api.batterymantra.entity.enums.DeliveryMethod.valueOf(request.getDeliveryMethod());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or missing Delivery Method");
        }
        
        com.api.batterymantra.entity.enums.PaymentMethod paymentMethod = null;
        try {
            if (request.getPaymentMethod() != null) {
                paymentMethod = com.api.batterymantra.entity.enums.PaymentMethod.valueOf(request.getPaymentMethod());
            } else {
                paymentMethod = com.api.batterymantra.entity.enums.PaymentMethod.COD; // Default to COD
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Payment Method");
        }

        //Creating a new Order
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
                    matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityId(cityId).orElse(null);
                }
            }

            // 2. Fallback: Try matching by Shipping Address City Name
            if (matchedPartner == null && address.getCity() != null && !address.getCity().isBlank()) {
                String cleanCity = address.getCity().trim();
                matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(cleanCity).orElse(null);
            }

            if (matchedPartner != null) {
                orders.setAssignedPartner(matchedPartner);
            }
        }

        //Calculating the Total Amount
        BigDecimal subTotal = orderItems.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //Calculate Exchange Discount
        BigDecimal exchangeDiscount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItemList) {
            if (cartItem.isExchangeOldBattery() && cartItem.getProduct().getExchangeDiscount() != null) {
                exchangeDiscount = exchangeDiscount.add(cartItem.getProduct().getExchangeDiscount().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }

        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        orders.setOrderItems(orderItems);
        orders.setTotalAmount(total);
        orders.setExchangeDiscount(exchangeDiscount);

        //Save the Order
        Orders placedOrder = orderRepository.save(orders);

        //Clearing Cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        return orderMapper.toOrderResponse(placedOrder);
    }

    @Transactional
    public OrderResponse createAdminOrder(AdminCreateOrderRequest request) {
        // Fetch User
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + request.getCustomerId()));

        // Fetch Address
        Address address;
        if (request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        } else {
            List<Address> addresses = addressRepository.findAllByUserUserIdAndIsDeletedFalse(customer.getUserId());
            if (addresses.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has no addresses to use as default");
            }
            address = addresses.get(0);
        }

        com.api.batterymantra.entity.enums.DeliveryMethod deliveryMethod;
        try {
            deliveryMethod = com.api.batterymantra.entity.enums.DeliveryMethod.valueOf(request.getDeliveryMethod());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Delivery Method");
        }

        com.api.batterymantra.entity.enums.PaymentMethod paymentMethod;
        try {
            paymentMethod = com.api.batterymantra.entity.enums.PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (Exception e) {
            paymentMethod = com.api.batterymantra.entity.enums.PaymentMethod.COD;
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
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + itemReq.getProductId()));

            OrderItems item = OrderItems.builder()
                    .order(orders)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .priceAtPurchase(product.getProductPrice())
                    .build();

            orderItems.add(item);
            subTotal = subTotal.add(product.getProductPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            if (itemReq.isExchangeOldBattery() && product.getExchangeDiscount() != null) {
                exchangeDiscount = exchangeDiscount.add(product.getExchangeDiscount().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
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
                    matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityId(cityId).orElse(null);
                }
            }
            if (matchedPartner == null && address.getCity() != null && !address.getCity().isBlank()) {
                String cleanCity = address.getCity().trim();
                matchedPartner = partnerProfileRepository.findFirstByIsActiveTrueAndOperatingCities_CityNameIgnoreCase(cleanCity).orElse(null);
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
        return orderMapper.toOrderResponse(placedOrder);
    }

    //To Get All the Orders Placed by a Customer
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID customerId) {
        List<Orders> orders = orderRepository.findByCustomer_UserIdOrderByPlacedAtDesc(customerId);

        if (orders.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No orders found for user: " + customerId);

        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }

    //To Get a Particular Order by ID
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
        return orderMapper.toOrderResponse(cancelledOrder);
    }

    // ===== ADMIN Methods =====

    /**
     * Get all orders in the system (ADMIN only)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
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

        // Validate status transition
        validateStatusTransition(order.getOrderStatus(), newStatus);

        // If cancelling, restore stock
        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            for (OrderItems item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setProductStock(product.getProductStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setOrderStatus(newStatus);
        Orders updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status of a delivered order");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status of a cancelled order");
        }
    }

    // ===== PARTNER Methods =====

    @Transactional(readOnly = true)
    public List<OrderResponse> getPartnerOrders(UUID partnerProfileId) {
        List<Orders> orders = orderRepository.findByAssignedPartner_IdOrderByPlacedAtDesc(partnerProfileId);
        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse updatePartnerOrderStatus(UUID orderId, OrderStatus newStatus, UUID partnerProfileId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (order.getAssignedPartner() == null || !order.getAssignedPartner().getId().equals(partnerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order. It is not assigned to you.");
        }

        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        Orders updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
    }


    private List<CartItem> getCartItems(UUID customerId, Cart cart) {
        List<CartItem> cartItemList = cart.getCartItems();

        //Check if cart is empty or not
        if (cartItemList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty for user: " + customerId);
        }

        //Checking Stock Availability
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Order is not assigned to your partner branch.");
        }

        EngineerProfile engineer = engineerProfileRepository.findById(engineerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found"));

        if (engineer.getPartnerProfile() == null || !engineer.getPartnerProfile().getId().equals(partnerProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only assign engineers belonging to your partner branch.");
        }

        order.setAssignedEngineer(engineer);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }
}
