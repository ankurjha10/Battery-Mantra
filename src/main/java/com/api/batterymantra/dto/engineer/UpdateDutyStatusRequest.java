package com.api.batterymantra.dto.engineer;

import com.api.batterymantra.entity.enums.DutyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDutyStatusRequest {

    @NotNull(message = "Duty status is required")
    private DutyStatus dutyStatus;
}
