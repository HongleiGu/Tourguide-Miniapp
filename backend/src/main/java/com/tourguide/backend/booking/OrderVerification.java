package com.tourguide.backend.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 核销记录 (MIN-33): one row per verified order. order_id is UNIQUE => 不可重复核销. */
@Getter
@Setter
@Entity
@Table(name = "order_verification")
public class OrderVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    /** The app-user (GUIDE role) who performed the 核销 — for traceability. */
    @Column(name = "guide_id")
    private Long guideId;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
