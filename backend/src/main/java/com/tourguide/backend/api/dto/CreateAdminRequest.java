package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body for POST /api/admin/admins (create an admin account). */
public record CreateAdminRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotEmpty List<String> roles) {
}
