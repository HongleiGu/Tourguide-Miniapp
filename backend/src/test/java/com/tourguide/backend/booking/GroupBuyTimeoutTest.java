package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.common.redis.DelayedTaskQueue;
import com.tourguide.backend.common.redis.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class GroupBuyTimeoutTest extends AbstractIntegrationTest {

    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    GroupBuyRepository groupBuyRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    TouristService touristService;
    @Autowired
    GroupBuyTimeoutWorker worker;
    @Autowired
    DelayedTaskQueue delayedQueue;

    private long newGroupSession(int capacity) {
        ScenicSession s = new ScenicSession();
        s.setTitle("超时拼团测试");
        s.setType("GROUP");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setCapacity(capacity);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        return sessionRepo.save(s).getId();
    }

    @Test
    void underMin_atDeadline_isVoided_andOrdersCancelled() {
        long sessionId = newGroupSession(5);
        OrderView order = touristService.createOrder(1L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
        GroupBuy g = groupBuyRepo.findBySessionId(sessionId).orElseThrow();
        assertThat(g.getCurrentSize()).isEqualTo(1); // < min (2)
        // force the deadline into the past -> backstop scan picks it up
        g.setDeadline(Instant.now().minusSeconds(60));
        groupBuyRepo.save(g);

        worker.run();

        assertThat(groupBuyRepo.findById(g.getId()).orElseThrow().getStatus()).isEqualTo("VOIDED");
        assertThat(orderRepo.findById(order.id()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void reachedMin_atDeadline_isConfirmed_andOrdersKept() {
        long sessionId = newGroupSession(5);
        OrderView order = touristService.createOrder(1L, new CreateOrderRequest(sessionId, 2, "t", "13800000000", null));
        GroupBuy g = groupBuyRepo.findBySessionId(sessionId).orElseThrow();
        assertThat(g.getCurrentSize()).isEqualTo(2); // >= min (2)
        g.setDeadline(Instant.now().minusSeconds(60));
        groupBuyRepo.save(g);

        worker.run();

        assertThat(groupBuyRepo.findById(g.getId()).orElseThrow().getStatus()).isEqualTo("CONFIRMED");
        assertThat(orderRepo.findById(order.id()).orElseThrow().getStatus()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void delayedQueueTrigger_voidsUnformedGroup() {
        long sessionId = newGroupSession(5);
        touristService.createOrder(1L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
        GroupBuy g = groupBuyRepo.findBySessionId(sessionId).orElseThrow();
        // deadline stays in the future -> the DB scan won't catch it; only the queue trigger will
        delayedQueue.schedule(RedisKeys.GROUP_BUY_TIMEOUT_QUEUE, String.valueOf(g.getId()), Duration.ZERO);

        worker.run();

        assertThat(groupBuyRepo.findById(g.getId()).orElseThrow().getStatus()).isEqualTo("VOIDED");
    }
}
