package com.tourguide.backend.common.redis;

import com.tourguide.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the distributed lock and delayed queue against a real Redis. */
class RedisIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    DistributedLock lock;

    @Autowired
    DelayedTaskQueue queue;

    @Test
    void lock_isExclusiveAcrossThreads_andReusableAfterRelease() throws Exception {
        String key = "lock:test:" + System.nanoTime();

        assertThat(lock.tryLock(key, Duration.ofMillis(100), Duration.ofSeconds(10))).isTrue();
        try {
            // a different thread must not acquire the same lock while it is held
            ExecutorService other = Executors.newSingleThreadExecutor();
            boolean acquiredByOther = other.submit(
                    () -> lock.tryLock(key, Duration.ofMillis(200), Duration.ofSeconds(5))).get();
            other.shutdown();
            assertThat(acquiredByOther).isFalse();
        } finally {
            lock.unlock(key);
        }

        // released -> acquirable again
        assertThat(lock.tryLock(key, Duration.ofMillis(100), Duration.ofSeconds(5))).isTrue();
        lock.unlock(key);
    }

    @Test
    void delayedQueue_deliversOnlyAfterDelay() throws Exception {
        String q = "delay:test:" + System.nanoTime();
        queue.schedule(q, "payload-1", Duration.ofSeconds(1));

        // not due immediately
        assertThat(queue.pollDue(q)).isEmpty();

        // becomes due within a couple of seconds
        List<String> got = List.of();
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            got = queue.pollDue(q);
            if (!got.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(got).containsExactly("payload-1");
        // drained
        assertThat(queue.pollDue(q)).isEmpty();
    }
}
