package com.api.batterymantra.dto.location;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPincodeRequest {
    @NotEmpty(message = "Pincodes list cannot be empty")
    private List<String> codes;
}
