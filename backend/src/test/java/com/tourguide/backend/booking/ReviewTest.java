package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.api.dto.ReviewRequest;
import com.tourguide.backend.api.dto.ReviewView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest extends AbstractIntegrationTest {

    @Autowired
    AppUserRepository userRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    TouristService touristService;

    private long user() {
        AppUser u = new AppUser();
        u.setNickname("评价用户");
        return userRepo.save(u).getId();
    }

    private long session() {
        ScenicSession s = new ScenicSession();
        s.setTitle("评价测试");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 0));
        s.setCapacity(5);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        return sessionRepo.save(s).getId();
    }

    private long completedOrder(long userId) {
        OrderView order = touristService.createOrder(userId,
                new CreateOrderRequest(session(), 1, "t", "13800000000", null));
        BookingOrder o = orderRepo.findById(order.id()).orElseThrow();
        o.setStatus("COMPLETED");
        orderRepo.save(o);
        return o.getId();
    }

    @Test
    void review_persistsAndIsReadable() {
        long userId = user();
        long orderId = completedOrder(userId);

        ReviewView review = touristService.reviewOrder(userId, orderId, new ReviewRequest(5, "讲解很棒"));

        assertThat(review.rating()).isEqualTo(5);
        assertThat(touristService.getReview(userId, orderId).content()).isEqualTo("讲解很棒");
    }

    @Test
    void review_requiresCompletedOrder() {
        long userId = user();
        OrderView pending = touristService.createOrder(userId,
                new CreateOrderRequest(session(), 1, "t", "13800000000", null));

        assertThatThrownBy(() -> touristService.reviewOrder(userId, pending.id(), new ReviewRequest(4, "x")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void review_isOnePerOrder() {
        long userId = user();
        long orderId = completedOrder(userId);
        touristService.reviewOrder(userId, orderId, new ReviewRequest(5, "first"));

        assertThatThrownBy(() -> touristService.reviewOrder(userId, orderId, new ReviewRequest(3, "again")))
                .isInstanceOf(BusinessException.class);
    }
}
