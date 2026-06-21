package com.tourguide.backend.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 价格与拼团规则 (MIN-49): one row per (session_type, day_type). */
@Getter
@Setter
@Entity
@Table(name = "pricing_rule")
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_type")
    private String sessionType;

    /** WORKDAY 工作日 / HOLIDAY 节假日. */
    @Column(name = "day_type")
    private String dayType;

    @Column(name = "price_fen")
    private Long priceFen = 0L;

    @Column(name = "group_min")
    private Integer groupMin;

    @Column(name = "group_max")
    private Integer groupMax;
}
