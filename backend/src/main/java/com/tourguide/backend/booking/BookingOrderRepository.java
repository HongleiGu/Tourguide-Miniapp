package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {

    Optional<BookingOrder> findByOrderNo(String orderNo);

    Optional<BookingOrder> findByVerifyCode(String verifyCode);

    List<BookingOrder> findByUserIdOrderByIdDesc(Long userId);

    List<BookingOrder> findByGuideIdOrderByIdDesc(Long guideId);

    List<BookingOrder> findByGuideIdIsNullAndStatusInOrderByIdDesc(List<String> statuses);

    int countByGuideIdAndVisitDateAndStatus(Long guideId, LocalDate visitDate, String status);

    /** 抢单: claim an unassigned order atomically. Returns 1 if won, 0 if already taken. */
    @Modifying(clearAutomatically = true)
    @Query("update BookingOrder o set o.guideId = :guideId where o.id = :id and o.guideId is null")
    int grab(@Param("id") Long id, @Param("guideId") Long guideId);

    /** Cancel a session's active orders (used when a group-buy is voided). Returns rows updated. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update BookingOrder o set o.status = 'CANCELLED'
            where o.sessionId = :sessionId and o.status in ('PENDING_PAYMENT', 'PAID')
            """)
    int cancelActiveBySession(@Param("sessionId") Long sessionId);

    /** People already committed to a session (paid/completed) — used to compute remaining seats. */
    @Query("""
            select coalesce(sum(o.peopleCount), 0) from BookingOrder o
            where o.sessionId = :sessionId and o.status in ('PAID', 'COMPLETED')
            """)
    int sumBookedPeople(@Param("sessionId") Long sessionId);
}
