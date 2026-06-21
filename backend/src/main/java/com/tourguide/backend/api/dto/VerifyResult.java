package com.tourguide.backend.api.dto;

/** Result of a successful 核销. */
public record VerifyResult(
        Long orderId,
        String orderNo,
        String sessionTitle,
        int peopleCount,
        String status,
        String verifiedAt) {
}
