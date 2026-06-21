package com.tourguide.backend.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/** A bookable guide session/场次 (private / group-buy / exclusive-slot). */
@Getter
@Setter
@Entity
@Table(name = "scenic_session")
public class ScenicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /** PRIVATE 私人 / GROUP 拼团 / EXCLUSIVE 专属时段. */
    private String type;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private Integer capacity;

    @Column(name = "guide_id")
    private Long guideId;

    @Column(name = "price_fen")
    private Long priceFen;

    /** OPEN / LOCKED / CLOSED / CANCELLED. */
    private String status;
}
