package com.tourguide.backend.guide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** 讲解员档案 (MIN-6). One per guide app_user. */
@Getter
@Setter
@Entity
@Table(name = "guide_profile")
public class GuideProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** SELF 自营 / OUTSOURCED 外包. */
    @Column(name = "employment_type")
    private String employmentType = "SELF";

    private String bio;

    /** 派单权重 (admin-set, used by MIN-7 dispatch). */
    @Column(name = "dispatch_weight")
    private Integer dispatchWeight = 100;

    private BigDecimal rating = new BigDecimal("5.00");

    @Column(name = "star_level")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer starLevel = 5;

    @Column(name = "reject_rate")
    private BigDecimal rejectRate = new BigDecimal("0.0000");

    /** 开启/关闭接单. */
    @Column(name = "accepting_orders")
    private Boolean acceptingOrders = true;

    /** ENABLED / DISABLED / SUSPENDED. */
    private String status = "ENABLED";
}
