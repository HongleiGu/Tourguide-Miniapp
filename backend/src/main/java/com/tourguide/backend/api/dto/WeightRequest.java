package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.Min;

/** Body for POST /api/admin/guides/{id}/dispatch-weight. */
public record WeightRequest(
        @Min(0) int weight) {
}
