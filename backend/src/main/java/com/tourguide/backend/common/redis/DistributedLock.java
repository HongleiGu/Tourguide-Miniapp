package com.tourguide.backend.common.redis;

import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Thin wrapper over Redisson distributed locks. Used by the group-buy engine (MIN-4) to make
 * "check remaining seats then claim" atomic across instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redisson;

    /**
     * Try to acquire {@code key}, waiting up to {@code wait}, auto-releasing after {@code lease}.
     *
     * @return true if the lock was acquired
     */
    public boolean tryLock(String key, Duration wait, Duration lease) {
        try {
            return redisson.getLock(key).tryLock(wait.toMillis(), lease.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted acquiring lock {}", key);
            return false;
        }
    }

    /** Release {@code key} if held by the current thread. */
    public void unlock(String key) {
        RLock lock = redisson.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Run {@code action} while holding {@code key}; always releases. Throws
     * {@link ErrorCode#CONFLICT} if the lock cannot be acquired within {@code wait}.
     */
    public <T> T executeWithLock(String key, Duration wait, Duration lease, Supplier<T> action) {
        if (!tryLock(key, wait, lease)) {
            throw new BusinessException(ErrorCode.CONFLICT, "操作繁忙，请稍后再试");
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }
}
