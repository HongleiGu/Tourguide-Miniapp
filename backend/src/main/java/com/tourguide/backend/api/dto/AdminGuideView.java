package com.tourguide.backend.api.dto;

/** Admin-facing view of a guide_profile (MIN-43; fuller management in MIN-8). */
public record AdminGuideView(
        Long guideId,
        String name,
        String employmentType,
        String status,
        boolean acceptingOrders,
        int dispatchWeight,
        double rating,
        int starLevel) {
}
