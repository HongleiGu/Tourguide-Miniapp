package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/admin/guides/{id}/suspend. */
public record SuspendRequest(
        @NotNull Boolean suspended) {
}
