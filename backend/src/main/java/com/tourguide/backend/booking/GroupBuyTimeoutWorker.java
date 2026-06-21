package com.tourguide.backend.booking;

import com.tourguide.backend.common.redis.DelayedTaskQueue;
import com.tourguide.backend.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Resolves due group-buys (成团 -> CONFIRMED, else VOIDED). Two triggers, both idempotent:
 * the Redis delayed queue (timely) and a DB scan of overdue groups (crash-safe backstop).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyTimeoutWorker {

    private final DelayedTaskQueue delayedQueue;
    private final GroupBuyRepository groupBuyRepo;
    private final GroupBuyService groupBuyService;

    @Scheduled(fixedDelayString = "${app.groupbuy.poll-interval-ms:5000}")
    public void run() {
        // 1) Timely: drain due timeout events from the Redis delayed queue.
        for (String payload : delayedQueue.pollDue(RedisKeys.GROUP_BUY_TIMEOUT_QUEUE)) {
            resolveSafely(payload);
        }
        // 2) Backstop: catch any overdue group the queue may have missed (restart / lost message).
        List<GroupBuy> overdue = groupBuyRepo.findByStatusInAndDeadlineLessThanEqual(
                List.of("FORMING", "LOCKED"), Instant.now());
        for (GroupBuy g : overdue) {
            resolveSafely(String.valueOf(g.getId()));
        }
    }

    private void resolveSafely(String groupBuyId) {
        try {
            groupBuyService.resolveAtDeadline(Long.parseLong(groupBuyId));
        } catch (RuntimeException e) {
            log.warn("group-buy timeout resolve failed for {}: {}", groupBuyId, e.getMessage());
        }
    }
}
