package com.tourguide.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Redis-backed delayed queue implemented as a sorted set (member = payload, score = due epoch-ms).
 * A payload only becomes visible once its due-time passes — the mechanism behind group-buy timeout
 * auto-void (MIN-4).
 *
 * <p>Producers call {@link #schedule}. A consumer — typically a scheduled worker added in MIN-4 —
 * periodically calls {@link #pollDue} and acts on each returned payload. This class is the transport
 * scaffold; the business handler lives with its feature.
 */
@Component
@RequiredArgsConstructor
public class DelayedTaskQueue {

    private final StringRedisTemplate redis;

    /** Atomically read and remove every member whose due-time (score) is &lt;= now. */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> POP_DUE = RedisScript.of("""
            local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            for i = 1, #due do redis.call('ZREM', KEYS[1], due[i]) end
            return due
            """, List.class);

    /** Schedule {@code payload} to become due on {@code queue} after {@code delay}. */
    public void schedule(String queue, String payload, Duration delay) {
        long dueAt = Instant.now().toEpochMilli() + delay.toMillis();
        redis.opsForZSet().add(queue, payload, dueAt);
    }

    /** Atomically fetch and remove all payloads on {@code queue} that are now due. */
    @SuppressWarnings("unchecked")
    public List<String> pollDue(String queue) {
        long now = Instant.now().toEpochMilli();
        List<String> due = redis.execute(POP_DUE, Collections.singletonList(queue), String.valueOf(now));
        return due == null ? List.of() : due;
    }
}
