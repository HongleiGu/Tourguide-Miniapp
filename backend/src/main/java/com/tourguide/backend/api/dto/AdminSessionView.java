package com.tourguide.backend.api.dto;

/** Admin-facing 场次 (with group-buy info for GROUP). */
public record AdminSessionView(
        Long id,
        String title,
        String type,
        String date,
        String startTime,
        String endTime,
        int capacity,
        long priceFen,
        Long guideId,
        String status,
        Integer groupMin,
        Integer groupMax,
        Integer groupCurrent) {
}
