package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/admin/orders/{id}/handle — 异常处理. */
public record OrderHandleRequest(
        @NotBlank String action,
        String reason) {
}
