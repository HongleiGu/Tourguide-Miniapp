package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotNull;

/** Body for PATCH /api/guide/accepting (开启/关闭接单). */
public record AcceptingRequest(
        @NotNull Boolean accepting) {
}
