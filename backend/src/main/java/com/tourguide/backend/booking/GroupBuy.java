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

/** Group-buy (拼团) state for one GROUP session. Seat count is the source of truth in MySQL. */
@Getter
@Setter
@Entity
@Table(name = "session_group_buy")
public class GroupBuy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "min_size")
    private Integer minSize;

    @Column(name = "max_size")
    private Integer maxSize;

    @Column(name = "current_size")
    private Integer currentSize = 0;

    /** FORMING (accepting joins) / LOCKED (full) / CONFIRMED (成团 finalized) / VOIDED (timed out). */
    private String status = "FORMING";

    /** 超时未成团作废 deadline (used by the auto-void worker in MIN-29). */
    private Instant deadline;

    public boolean isFormed() {
        return currentSize != null && minSize != null && currentSize >= minSize;
    }
}
