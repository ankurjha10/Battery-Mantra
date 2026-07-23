package com.api.batterymantra.dto.faq;

import com.api.batterymantra.enums.FaqPageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaqRequest {

    @NotNull(message = "Page type is required")
    private FaqPageType pageType;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;
    
    private Boolean isActive = true;
}
