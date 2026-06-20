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
import java.time.LocalDate;

/** A tourist booking order. */
@Getter
@Setter
@Entity
@Table(name = "booking_order")
public class BookingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "guide_id")
    private Long guideId;

    private String type;

    @Column(name = "people_count")
    private Integer peopleCount;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "amount_fen")
    private Long amountFen;

    /** PENDING_PAYMENT / PAID / COMPLETED / CANCELLED / REFUNDED. */
    private String status;

    @Column(name = "verify_code")
    private String verifyCode;

    @Column(name = "paid_at")
    private Instant paidAt;
}
