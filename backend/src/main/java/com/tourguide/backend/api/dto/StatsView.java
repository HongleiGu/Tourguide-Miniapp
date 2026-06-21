package com.tourguide.backend.api.dto;

/** 基础统计 (MIN-51) for a period. Rates are fractions 0..1. */
public record StatsView(
        String from,
        String to,
        int totalOrders,
        int paidOrders,
        int completedOrders,
        int visitors,
        long revenueFen,
        double groupFormationRate,
        double verificationRate) {
}
