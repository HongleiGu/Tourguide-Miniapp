package com.tourguide.backend.api.dto;

/** The logged-in guide's profile, returned by GET /api/guide/me. */
public record GuideMe(
        Long guideId,
        String name,
        String employmentType,
        boolean acceptingOrders,
        String status,
        double rating,
        int starLevel) {
}
