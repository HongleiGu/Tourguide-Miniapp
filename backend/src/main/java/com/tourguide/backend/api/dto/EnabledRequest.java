package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/admin/guides/{id}/enabled (启用/禁用). */
public record EnabledRequest(
        @NotNull Boolean enabled) {
}
