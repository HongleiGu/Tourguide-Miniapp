package com.tourguide.backend.api.dto;

/** Admin-facing order row (查询/筛选). */
public record AdminOrderView(
        Long id,
        String orderNo,
        String type,
        String status,
        int peopleCount,
        long amountFen,
        String visitDate,
        Long guideId,
        Long userId,
        String sessionTitle,
        String contactName,
        String contactPhone) {
}
