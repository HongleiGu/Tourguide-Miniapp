package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {

    Optional<GroupBuy> findBySessionId(Long sessionId);

    /** Overdue groups still awaiting resolution (backstop scan for the timeout worker). */
    List<GroupBuy> findByStatusInAndDeadlineLessThanEqual(Collection<String> statuses, Instant deadline);

    /** 成团: finalize a group that reached min. Idempotent. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.status = 'CONFIRMED'
            where g.id = :id and g.status in ('FORMING', 'LOCKED') and g.currentSize >= g.minSize
            """)
    int confirm(@Param("id") Long id);

    /** 超时未成团作废: void a group that never reached min. Idempotent. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.status = 'VOIDED'
            where g.id = :id and g.status = 'FORMING' and g.currentSize < g.minSize
            """)
    int voidIfNotFormed(@Param("id") Long id);

    /**
     * Atomically claim {@code n} seats. The {@code current_size + n <= max_size} guard makes
     * overselling impossible regardless of concurrency. Returns the number of rows updated
     * (1 = claimed, 0 = full/closed).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.currentSize = g.currentSize + :n
            where g.id = :id and g.status = 'FORMING' and g.currentSize + :n <= g.maxSize
            """)
    int claimSeats(@Param("id") Long id, @Param("n") int n);

    /** Lock the group once it is full (no more joins). Idempotent. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.status = 'LOCKED'
            where g.id = :id and g.status = 'FORMING' and g.currentSize >= g.maxSize
            """)
    int lockIfFull(@Param("id") Long id);

    /** Release {@code n} seats back (on cancellation). Guarded so it can't go negative. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.currentSize = g.currentSize - :n
            where g.id = :id and g.currentSize >= :n and g.status in ('FORMING', 'LOCKED')
            """)
    int releaseSeats(@Param("id") Long id, @Param("n") int n);

    /** Reopen a previously-full group once a seat frees up. Idempotent. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update GroupBuy g set g.status = 'FORMING'
            where g.id = :id and g.status = 'LOCKED' and g.currentSize < g.maxSize
            """)
    int reopenIfNotFull(@Param("id") Long id);
}
