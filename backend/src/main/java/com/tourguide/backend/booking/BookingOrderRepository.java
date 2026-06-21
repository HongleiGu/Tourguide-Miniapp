package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {

    Optional<BookingOrder> findByOrderNo(String orderNo);

    /** People already committed to a session (paid/completed) — used to compute remaining seats. */
    @Query("""
            select coalesce(sum(o.peopleCount), 0) from BookingOrder o
            where o.sessionId = :sessionId and o.status in ('PAID', 'COMPLETED')
            """)
    int sumBookedPeople(@Param("sessionId") Long sessionId);
}
