package com.api.batterymantra.dto.order;

import com.api.batterymantra.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "Order status must not be null")
    private OrderStatus orderStatus;
}
