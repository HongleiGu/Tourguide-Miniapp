package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Body for POST /api/tourist/orders/{id}/review. 图片 deferred until object storage. */
public record ReviewRequest(
        @Min(1) @Max(5) int rating,
        String content) {
}
