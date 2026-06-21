package com.tourguide.backend.api.dto;

/** One 排班 segment (WORK/REST) for a day. */
public record ScheduleSegment(
        Long id,
        String date,
        String type,
        String startTime,
        String endTime) {
}
