package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/refresh. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
