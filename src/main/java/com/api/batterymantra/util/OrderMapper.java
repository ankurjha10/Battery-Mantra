package com.api.batterymantra.util;

import com.api.batterymantra.dto.order.OrderItemResponse;
import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.entity.Orders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Orders order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderStatus(order.getOrderStatus().name());
        response.setTotalAmount(order.getTotalAmount());
        response.setPlacedAt(order.getPlacedAt());
        if (order.getShippingAddress() != null) {
            response.setShippingAddress(
                    order.getShippingAddress().getAddressLine1()
                            + ", " + order.getShippingAddress().getAddressLine2()
                            + ", " + order.getShippingAddress().getCity()
                            + ", " + order.getShippingAddress().getState()
                            + ", " + order.getShippingAddress().getPostalCode()
                            + ", " + order.getShippingAddress().getCountry()
            );
        }
        if (order.getDeliveryMethod() != null) {
            response.setDeliveryMethod(order.getDeliveryMethod().name());
        }
        if (order.getPaymentMethod() != null) {
            response.setPaymentMethod(order.getPaymentMethod().name());
        }
        response.setInstallationDate(order.getInstallationDate());
        response.setExchangeDiscount(order.getExchangeDiscount());
        
        if (order.getShippingAddress() != null) {
            response.setCustomerName(order.getShippingAddress().getFullName());
            response.setCustomerPhone(order.getShippingAddress().getPhoneNumber());
        } else if (order.getCustomer() != null) {
            response.setCustomerName(order.getCustomer().getUsername());
            response.setCustomerPhone(order.getCustomer().getPhoneNumber());
        }
        
        if (order.getCustomer() != null) {
            response.setCustomerEmail(order.getCustomer().getEmail());
        }
        response.setOrderItems(order.getOrderItems().stream().map(item -> {
            OrderItemResponse itemResponse = new OrderItemResponse();
            itemResponse.setProductId(item.getProduct().getProductId());
            itemResponse.setProductName(item.getProduct().getProductName());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setProductImage(item.getProduct().getProductImage());
            itemResponse.setPriceAtPurchase(item.getPriceAtPurchase());
            itemResponse.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemResponse;
        }).toList());
        if (order.getAssignedPartner() != null) {
            com.api.batterymantra.dto.user.PartnerResponse p = com.api.batterymantra.dto.user.PartnerResponse.builder()
                    .id(order.getAssignedPartner().getId())
                    .businessName(order.getAssignedPartner().getBusinessName())
                    .build();
            response.setAssignedPartner(p);
        }
        
        if (order.getAssignedEngineer() != null) {
            com.api.batterymantra.dto.user.EngineerResponse e = com.api.batterymantra.dto.user.EngineerResponse.builder()
                    .id(order.getAssignedEngineer().getId())
                    .fullName(order.getAssignedEngineer().getFullName())
                    .build();
            response.setAssignedEngineer(e);
        }

        return response;
    }
}
