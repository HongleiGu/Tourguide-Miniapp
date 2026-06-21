package com.tourguide.backend.api.dto;

import java.util.List;

/** 工作台看板 (MIN-36) for the logged-in guide, for today. */
public record GuideWorkbench(
        String date,
        boolean accepting,
        boolean onDutyToday,
        int pendingAcceptCount,
        int toVerifyCount,
        int completedCount,
        int remainingCapacity,
        List<ScheduleSegment> schedule) {
}
