package com.api.batterymantra.dto.engineer;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateLeaveRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
