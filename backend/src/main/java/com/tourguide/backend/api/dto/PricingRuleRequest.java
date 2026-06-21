package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for PUT /api/admin/pricing (upsert a rule). */
public record PricingRuleRequest(
        @NotBlank String sessionType,
        @NotBlank String dayType,
        @NotNull @Min(0) Long priceFen,
        Integer groupMin,
        Integer groupMax) {
}
