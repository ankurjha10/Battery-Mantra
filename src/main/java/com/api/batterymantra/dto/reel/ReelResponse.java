package com.api.batterymantra.dto.reel;

import lombok.Data;
import java.util.UUID;

@Data
public class ReelResponse {
    private UUID reelId;
    private String url;
    private Boolean isActive;
    private Integer displayOrder;
}
