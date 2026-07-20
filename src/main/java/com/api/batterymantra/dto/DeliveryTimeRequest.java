package com.api.batterymantra.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DeliveryTimeRequest {
    private UUID categoryId;
    private UUID cityId;
    private String days;
    private String hours;
}
