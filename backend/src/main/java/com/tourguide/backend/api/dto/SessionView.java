package com.tourguide.backend.api.dto;

/** A bookable session as shown on the tourist home. */
public record SessionView(
        Long id,
        String title,
        String type,
        String date,
        String startTime,
        String endTime,
        int capacity,
        int remaining,
        long priceFen) {
}
