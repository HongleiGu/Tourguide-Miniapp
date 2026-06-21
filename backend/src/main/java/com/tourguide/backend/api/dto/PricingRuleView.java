package com.tourguide.backend.api.dto;

/** A pricing rule row (per session-type x day-type). */
public record PricingRuleView(
        Long id,
        String sessionType,
        String dayType,
        long priceFen,
        Integer groupMin,
        Integer groupMax) {
}
