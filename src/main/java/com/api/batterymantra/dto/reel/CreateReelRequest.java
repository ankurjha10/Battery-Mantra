package com.api.batterymantra.dto.reel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReelRequest {
    @NotBlank(message = "Reel URL is required")
    private String url;
    
    private Boolean isActive = true;
    
    private Integer displayOrder;
}
