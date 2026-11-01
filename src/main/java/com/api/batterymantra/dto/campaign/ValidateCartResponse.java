package com.api.batterymantra.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCartResponse {

    private boolean applicable;
    private UUID campaignId;
    private String campaignName;
    private BigDecimal discountAmount;
    private BigDecimal finalPayable;
    private String message;
}
