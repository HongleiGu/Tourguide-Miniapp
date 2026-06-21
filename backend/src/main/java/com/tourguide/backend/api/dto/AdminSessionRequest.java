package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Body for create/update of a 场次. */
public record AdminSessionRequest(
        @NotBlank String title,
        @NotBlank String type,
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        @NotNull Long priceFen,
        Long guideId,
        Integer groupMinSize,
        Integer groupMaxSize) {
}
