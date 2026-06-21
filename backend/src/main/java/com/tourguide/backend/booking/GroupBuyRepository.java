package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {

    Optional<GroupBuy> findBySessionId(Long sessionId);

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
}
