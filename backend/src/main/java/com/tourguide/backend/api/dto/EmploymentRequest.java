package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/admin/guides/{id}/employment (自营/外包). */
public record EmploymentRequest(
        @NotBlank String employmentType) {
}
