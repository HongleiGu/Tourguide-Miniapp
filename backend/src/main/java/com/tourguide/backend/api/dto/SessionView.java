package com.tourguide.backend.api.dto;

/**
 * A bookable session as shown on the tourist home. For GROUP (拼团) sessions, {@code joined}
 * and {@code groupStatus} carry the live group-buy state; both are null for other types.
 */
public record SessionView(
        Long id,
        String title,
        String type,
        String date,
        String startTime,
        String endTime,
        int capacity,
        int remaining,
        long priceFen,
        Integer joined,
        String groupStatus) {
}
