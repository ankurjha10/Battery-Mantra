package com.api.batterymantra.dto.campaign;

import com.api.batterymantra.dto.product.ProductListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLandingResponse {

    private CampaignResponse campaign;
    private Page<ProductListResponse> products;
}
