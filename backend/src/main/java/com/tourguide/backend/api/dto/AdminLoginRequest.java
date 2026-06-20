package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/admin/login. */
public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {
}
