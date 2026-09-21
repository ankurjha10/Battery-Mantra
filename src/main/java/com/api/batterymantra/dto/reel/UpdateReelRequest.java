package com.api.batterymantra.dto.reel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateReelRequest {
    @NotBlank(message = "Reel URL is required")
    private String url;
    
    private Boolean isActive;
    
    private Integer displayOrder;
}
