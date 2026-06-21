package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class GroupBuyConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    ScenicSessionRepository sessionRepo;

    @Autowired
    GroupBuyRepository groupBuyRepo;

    @Autowired
    TouristService touristService;

    @Test
    void parallelJoins_neverOversell_andLockAtFull() throws Exception {
        // A GROUP session with exactly 5 seats.
        ScenicSession s = new ScenicSession();
        s.setTitle("并发拼团测试");
        s.setType("GROUP");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setCapacity(5);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        final long sessionId = sessionRepo.save(s).getId();

        GroupBuy g = new GroupBuy();
        g.setSessionId(sessionId);
        g.setMinSize(2);
        g.setMaxSize(5);
        g.setCurrentSize(0);
        g.setStatus("FORMING");
        groupBuyRepo.save(g);

        // 20 threads each try to claim 1 seat at the same instant.
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    touristService.createOrder(1L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
                    return true;
                } catch (BusinessException e) {
                    return false; // full / closed — expected for the losers
                }
            }));
        }
        start.countDown();

        int success = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get())) {
                success++;
            }
        }
        pool.shutdown();

        // Exactly capacity claims succeed — never more (no oversell).
        assertThat(success).isEqualTo(5);
        GroupBuy finalGroup = groupBuyRepo.findBySessionId(sessionId).orElseThrow();
        assertThat(finalGroup.getCurrentSize()).isEqualTo(5);
        assertThat(finalGroup.getStatus()).isEqualTo("LOCKED");
    }
}
