package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelOrderTest extends AbstractIntegrationTest {

    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    GroupBuyRepository groupBuyRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    TouristService touristService;

    private long session(String type, int capacity) {
        ScenicSession s = new ScenicSession();
        s.setTitle("取消测试");
        s.setType(type);
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setCapacity(capacity);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        return sessionRepo.save(s).getId();
    }

    @Test
    void cancelGroupOrder_releasesSeat() {
        long sessionId = session("GROUP", 5);
        OrderView order = touristService.createOrder(1L, new CreateOrderRequest(sessionId, 2, "t", "13800000000", null));
        assertThat(groupBuyRepo.findBySessionId(sessionId).orElseThrow().getCurrentSize()).isEqualTo(2);

        OrderView cancelled = touristService.cancelOrder(1L, order.id());

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(groupBuyRepo.findBySessionId(sessionId).orElseThrow().getCurrentSize()).isEqualTo(0);
    }

    @Test
    void cancelPaidOrder_isRefunded() {
        long sessionId = session("PRIVATE", 1);
        OrderView order = touristService.createOrder(1L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
        touristService.mockPay(1L, order.id());

        assertThat(touristService.cancelOrder(1L, order.id()).status()).isEqualTo("REFUNDED");
    }

    @Test
    void cancelCompletedOrder_isRejected() {
        long sessionId = session("PRIVATE", 1);
        OrderView order = touristService.createOrder(1L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
        BookingOrder o = orderRepo.findById(order.id()).orElseThrow();
        o.setStatus("COMPLETED");
        orderRepo.save(o);

        assertThatThrownBy(() -> touristService.cancelOrder(1L, order.id()))
                .isInstanceOf(BusinessException.class);
    }
}
