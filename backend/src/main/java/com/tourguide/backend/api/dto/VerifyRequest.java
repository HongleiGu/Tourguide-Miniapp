package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/verify — the 核销码 the tourist presents. */
public record VerifyRequest(
        @NotBlank String code) {
}
