package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/admin/guides (新增讲解员). */
public record CreateGuideRequest(
        @NotBlank String name,
        String employmentType) {
}
