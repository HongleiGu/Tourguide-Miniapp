package com.tourguide.backend.booking;

import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.common.redis.DelayedTaskQueue;
import com.tourguide.backend.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * 拼团 engine. Seat claiming is an atomic, guarded conditional UPDATE in MySQL (no oversell,
 * no distributed lock); the order INSERT happens in the same transaction (see TouristService).
 * Timeouts are scheduled on the Redis delayed queue and resolved idempotently (MIN-29).
 */
@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private static final int DEFAULT_MIN_SIZE = 2;
    private static final Set<String> ACTIVE = Set.of("FORMING", "LOCKED");

    private final GroupBuyRepository groupBuyRepo;
    private final BookingOrderRepository orderRepo;
    private final DelayedTaskQueue delayedQueue;

    @Value("${app.groupbuy.timeout:PT30M}")
    private Duration groupTimeout;

    /** The group-buy for a GROUP session, created (with a deadline + scheduled timeout) on first access. */
    @Transactional
    public GroupBuy getOrCreate(ScenicSession session) {
        return groupBuyRepo.findBySessionId(session.getId())
                .orElseGet(() -> openGroup(session.getId(), DEFAULT_MIN_SIZE,
                        session.getCapacity() != null ? session.getCapacity() : 10));
    }

    /** Create a FORMING group-buy with a deadline and a scheduled timeout on the delayed queue. */
    @Transactional
    public GroupBuy openGroup(long sessionId, int minSize, int maxSize) {
        GroupBuy g = new GroupBuy();
        g.setSessionId(sessionId);
        g.setMinSize(minSize);
        g.setMaxSize(maxSize);
        g.setCurrentSize(0);
        g.setStatus("FORMING");
        g.setDeadline(Instant.now().plus(groupTimeout));
        GroupBuy saved = groupBuyRepo.save(g);
        delayedQueue.schedule(RedisKeys.GROUP_BUY_TIMEOUT_QUEUE, String.valueOf(saved.getId()), groupTimeout);
        return saved;
    }

    /**
     * Atomically claim {@code n} seats; lock the group if it becomes full.
     * Throws {@link ErrorCode#CONFLICT} when there aren't enough seats / the group is closed.
     */
    @Transactional
    public GroupBuy claim(long groupBuyId, int n) {
        if (groupBuyRepo.claimSeats(groupBuyId, n) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "拼团名额不足或已截止");
        }
        groupBuyRepo.lockIfFull(groupBuyId);
        return groupBuyRepo.findById(groupBuyId).orElseThrow();
    }

    /** Release seats back to a session's group (on order cancellation); reopen if it was full. */
    @Transactional
    public void release(long sessionId, int n) {
        groupBuyRepo.findBySessionId(sessionId).ifPresent(g -> {
            groupBuyRepo.releaseSeats(g.getId(), n);
            groupBuyRepo.reopenIfNotFull(g.getId());
        });
    }

    /**
     * Resolve a group at its deadline: 成团 (>= min) -> CONFIRMED; otherwise -> VOIDED and its
     * active orders cancelled. Idempotent (guarded UPDATEs) — safe to call repeatedly / from
     * both the delayed-queue trigger and the DB backstop scan.
     */
    @Transactional
    public void resolveAtDeadline(long groupBuyId) {
        GroupBuy g = groupBuyRepo.findById(groupBuyId).orElse(null);
        if (g == null || !ACTIVE.contains(g.getStatus())) {
            return;
        }
        if (g.getCurrentSize() >= g.getMinSize()) {
            groupBuyRepo.confirm(groupBuyId);
        } else if (groupBuyRepo.voidIfNotFormed(groupBuyId) > 0) {
            orderRepo.cancelActiveBySession(g.getSessionId());
        }
    }
}
