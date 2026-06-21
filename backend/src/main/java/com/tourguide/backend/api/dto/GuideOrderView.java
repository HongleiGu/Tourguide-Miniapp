package com.tourguide.backend.api.dto;

/** An order assigned to the logged-in guide (list + detail). */
public record GuideOrderView(
        Long id,
        String orderNo,
        String sessionTitle,
        String type,
        String date,
        String startTime,
        String endTime,
        int peopleCount,
        String status,
        String contactName,
        String contactPhone) {
}
