package com.tourguide.backend.api.dto;

import java.util.List;

/** 收入 (MIN-40): gross order count + total for the logged-in guide. 线上不做分佣. */
public record GuideIncome(
        int orderCount,
        long totalFen,
        List<Item> items) {

    public record Item(
            Long orderId,
            String orderNo,
            String sessionTitle,
            String date,
            long amountFen,
            String status) {
    }
}
