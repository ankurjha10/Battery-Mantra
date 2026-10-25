package com.api.batterymantra.util;

import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.dto.user.PartnerResponse;

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
        if (order.getOrderStatus() != null) {
            response.setOrderStatus(order.getOrderStatus().name());
        }
        response.setTotalAmount(order.getTotalAmount());
        response.setPlacedAt(order.getPlacedAt());
        if (order.getShippingAddress() != null) {
            com.api.batterymantra.entity.Address addr = order.getShippingAddress();
            response.setAddressLine1(addr.getAddressLine1());
            response.setAddressLine2(addr.getAddressLine2());
            response.setCity(addr.getCity());
            response.setState(addr.getState());
            response.setPostalCode(addr.getPostalCode());

            java.util.List<String> parts = new java.util.ArrayList<>();
            if (addr.getAddressLine1() != null && !addr.getAddressLine1().isBlank()) {
                parts.add(addr.getAddressLine1().trim());
            }
            if (addr.getAddressLine2() != null && !addr.getAddressLine2().isBlank() && !"null".equalsIgnoreCase(addr.getAddressLine2().trim())) {
                parts.add(addr.getAddressLine2().trim());
            }
            if (addr.getCity() != null && !addr.getCity().isBlank()) {
                parts.add(addr.getCity().trim());
            }
            String stateWithPin = (addr.getState() != null && !addr.getState().isBlank() ? addr.getState().trim() : "") +
                    (addr.getPostalCode() != null && !addr.getPostalCode().isBlank() ? " " + addr.getPostalCode().trim() : "");
            if (!stateWithPin.isBlank()) {
                parts.add(stateWithPin.trim());
            }
            response.setShippingAddress(String.join(", ", parts));
        }
        if (order.getDeliveryMethod() != null) {
            response.setDeliveryMethod(order.getDeliveryMethod().name());
        }
        if (order.getPaymentMethod() != null) {
            response.setPaymentMethod(order.getPaymentMethod().name());
        }
        if (order.getPaymentStatus() != null) {
            response.setPaymentStatus(order.getPaymentStatus().name());
        }
        response.setPaymentMode(order.getPaymentMode());
        response.setInstallationDate(order.getInstallationDate());
        response.setExchangeDiscount(order.getExchangeDiscount());
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_SUB_ADMIN") || a.getAuthority().equals("SUB_ADMIN"))) {
            response.setDeliverySecurityCode(order.getDeliverySecurityCode());
        } else {
            response.setDeliverySecurityCode(null);
        }

        // Completion fields
        response.setCompletedAt(order.getCompletedAt());
        response.setEngineerNotes(order.getEngineerNotes());
        response.setInstalledBatterySerialNumber(order.getInstalledBatterySerialNumber());
        response.setOldBatteryCollected(order.isOldBatteryCollected());
        response.setOldBatteryDetails(order.getOldBatteryDetails());
        response.setCancellationReason(order.getCancellationReason());
        response.setCancelledBy(order.getCancelledBy());
        response.setCouponCode(order.getCouponCode());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setSelectedCity(order.getSelectedCity());

        if (order.getShippingAddress() != null) {
            response.setCustomerName(order.getShippingAddress().getFullName());
            response.setCustomerPhone(order.getShippingAddress().getPhoneNumber());
            response.setLatitude(order.getShippingAddress().getLatitude());
            response.setLongitude(order.getShippingAddress().getLongitude());
        } else if (order.getCustomer() != null) {
            response.setCustomerName(order.getCustomer().getUsername());
            response.setCustomerPhone(order.getCustomer().getPhoneNumber());
        }

        if (order.getCustomer() != null) {
            response.setCustomerEmail(order.getCustomer().getEmail());
        }
        response.setOrderItems(order.getOrderItems().stream().map(item -> {
            OrderItemResponse itemResponse = new OrderItemResponse();
            if (item.getProduct() != null) {
                itemResponse.setProductId(item.getProduct().getProductId());
                itemResponse.setProductName(item.getProduct().getProductName());
                itemResponse.setProductImage(item.getProduct().getProductImage());
            }
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setPriceAtPurchase(item.getPriceAtPurchase());
            if (item.getPriceAtPurchase() != null) {
                itemResponse.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                itemResponse.setSubtotal(BigDecimal.ZERO);
            }
            return itemResponse;
        }).toList());

        if (order.getAssignedPartner() != null) {
            PartnerResponse p = PartnerResponse.builder()
                    .id(order.getAssignedPartner().getId())
                    .businessName(order.getAssignedPartner().getBusinessName())
                    .contactPerson(order.getAssignedPartner().getContactPerson())
                    .phoneNumber(order.getAssignedPartner().getUser() != null ? order.getAssignedPartner().getUser().getPhoneNumber() : null)
                    .build();
            response.setAssignedPartner(p);
        }

        if (order.getAssignedEngineer() != null) {
            EngineerResponse e = EngineerResponse.builder()
                    .id(order.getAssignedEngineer().getId())
                    .firstName(order.getAssignedEngineer().getFirstName())
                    .lastName(order.getAssignedEngineer().getLastName())
                    .phoneNumber(order.getAssignedEngineer().getUser() != null ? order.getAssignedEngineer().getUser().getPhoneNumber() : null)
                    .dutyStatus(order.getAssignedEngineer().getDutyStatus() != null
                            ? order.getAssignedEngineer().getDutyStatus().name() : null)
                    .build();
            response.setAssignedEngineer(e);
        }

        return response;
    }
}
