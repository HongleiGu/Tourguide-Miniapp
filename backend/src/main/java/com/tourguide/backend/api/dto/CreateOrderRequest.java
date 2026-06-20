package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Body for POST /api/tourist/orders. */
public record CreateOrderRequest(
        @NotNull Long sessionId,
        @Min(1) int peopleCount,
        String contactName,
        String contactPhone,
        LocalDate visitDate) {
}
