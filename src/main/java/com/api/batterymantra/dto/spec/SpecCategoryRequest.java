package com.api.batterymantra.dto.spec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SpecCategoryRequest {

    @NotBlank
    private String name;

    @NotNull
    private UUID categoryId;
}
