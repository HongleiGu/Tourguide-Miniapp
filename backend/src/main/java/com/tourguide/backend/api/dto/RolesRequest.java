package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body for POST /api/admin/admins/{id}/roles (assign roles). */
public record RolesRequest(
        @NotEmpty List<String> roles) {
}
