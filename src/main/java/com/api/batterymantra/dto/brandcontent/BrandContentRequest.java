package com.api.batterymantra.dto.brandcontent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class BrandContentRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Brand ID is required")
    private UUID brandId;

    private String normalShortDescription;
    private String normalDescription;
    private String normalSeoTitle;
    private String dealerShortDescription;
    private String dealerDescription;
    private String dealerSeoTitle;
}
