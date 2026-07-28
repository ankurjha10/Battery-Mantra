package com.api.batterymantra.dto.spec;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SpecCategoryResponse {

    private UUID id;
    private String name;
    private UUID categoryId;
    private String categoryName;
}
