package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/admin/sessions/{id}/status — OPEN / LOCKED (锁场) / CLOSED (停场). */
public record SessionStatusRequest(
        @NotBlank String status) {
}
