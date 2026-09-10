package com.api.batterymantra.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QrCodeResponse {

    private String qrCodeId;
    private String imageUrl;
}
