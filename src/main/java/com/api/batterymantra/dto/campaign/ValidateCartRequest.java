package com.api.batterymantra.dto.campaign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCartRequest {

    @NotEmpty(message = "Cart items must not be empty")
    @Valid
    private List<CartValidateItemDto> items;

    private String couponCode;

    private UUID cityId;
}
