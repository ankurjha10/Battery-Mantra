package com.api.batterymantra.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    private String code;
    private Double cartTotal;
    private List<ApplyCouponCartItemDto> cartItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplyCouponCartItemDto {
        private String productId;
        private String categoryId;
        private String brandId;
        private Double productPrice;
        private Integer quantity;
    }
}
