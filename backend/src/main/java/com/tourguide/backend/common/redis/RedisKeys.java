package com.tourguide.backend.common.redis;

/**
 * Central registry of Redis key conventions. Keep ALL key strings here so namespaces stay
 * consistent and collisions are easy to spot.
 *
 * <p>Convention: {@code <kind>:<domain>:<id...>} where kind is one of
 * {@code lock} (distributed locks), {@code cache} (cached reads), {@code delay} (delayed queues).
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    // ---- locks ----
    /** Lock guarding seat changes for one group-buy session (MIN-4 lock-at-full). */
    public static String groupBuyLock(long sessionId) {
        return "lock:groupbuy:" + sessionId;
    }

    // ---- cache ----
    /** Hot seat count for a group-buy session. */
    public static String sessionSeats(long sessionId) {
        return "cache:session:seats:" + sessionId;
    }

    // ---- delayed queues ----
    /** Queue carrying group-buy timeout events to auto-void unfulfilled sessions (MIN-4). */
    public static final String GROUP_BUY_TIMEOUT_QUEUE = "delay:groupbuy:timeout";
}
