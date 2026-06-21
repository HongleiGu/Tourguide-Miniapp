package com.tourguide.backend.booking;

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

import java.time.Instant;

/** 评价 (MIN-34): one review per completed order. 图片 (images) deferred until object storage. */
@Getter
@Setter
@Entity
@Table(name = "order_review")
public class OrderReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    /** The reviewed guide (may be null until guides are assigned). */
    @Column(name = "guide_id")
    private Long guideId;

    /** 评分 1-5 (stored as TINYINT). */
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer rating;

    /** 文字评价. */
    private String content;

    @Column(name = "created_at")
    private Instant createdAt;
}
