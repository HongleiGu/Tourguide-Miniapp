package com.tourguide.backend.api.dto;

/** An order returned to the tourist (incl. the 核销码 once paid). */
public record OrderView(
        Long id,
        String orderNo,
        String type,
        int peopleCount,
        long amountFen,
        String status,
        String verifyCode,
        Long sessionId,
        String sessionTitle) {
}
