package com.tourguide.backend.api.dto;

/** A submitted review. */
public record ReviewView(
        Long orderId,
        int rating,
        String content,
        String createdAt) {
}
