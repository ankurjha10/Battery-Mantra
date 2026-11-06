package com.api.batterymantra.dto.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBlogStatusRequest {

    @NotBlank(message = "Status is required")
    private String status; // DRAFT or PUBLISHED
}

