package com.tourguide.backend.dispatch;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.booking.BookingOrderRepository;
import com.tourguide.backend.booking.ScenicSession;
import com.tourguide.backend.booking.ScenicSessionRepository;
import com.tourguide.backend.booking.TouristService;
import com.tourguide.backend.guide.GuideProfile;
import com.tourguide.backend.guide.GuideProfileRepository;
import com.tourguide.backend.guide.GuideSchedule;
import com.tourguide.backend.guide.GuideScheduleRepository;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchTest extends AbstractIntegrationTest {

    @Autowired
    AppUserRepository userRepo;
    @Autowired
    GuideProfileRepository guideRepo;
    @Autowired
    GuideScheduleRepository scheduleRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    TouristService touristService;

    private GuideProfile guideWeighted(long id, int weight) {
        GuideProfile g = new GuideProfile();
        g.setId(id);
        g.setDispatchWeight(weight);
        return g;
    }

    @Test
    void pickWeighted_isProportionalAndDeterministic() {
        List<GuideProfile> guides = List.of(guideWeighted(1L, 10), guideWeighted(2L, 90)); // total 100
        assertThat(DispatchService.pickWeighted(guides, 0.0).getId()).isEqualTo(1L);
        assertThat(DispatchService.pickWeighted(guides, 0.05).getId()).isEqualTo(1L); // target 5 < 10
        assertThat(DispatchService.pickWeighted(guides, 0.5).getId()).isEqualTo(2L);  // target 50 in (10,100]
        assertThat(DispatchService.pickWeighted(guides, 0.99).getId()).isEqualTo(2L);
    }

    private long tourist() {
        AppUser u = new AppUser();
        u.setNickname("派单游客");
        return userRepo.save(u).getId();
    }

    private long eligibleGuideToday() {
        AppUser u = new AppUser();
        u.setNickname("派单讲解员");
        long uid = userRepo.save(u).getId();
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
        p.setAcceptingOrders(true);
        p.setStatus("ENABLED");
        long gid = guideRepo.save(p).getId();
        GuideSchedule s = new GuideSchedule();
        s.setGuideId(gid);
        s.setWorkDate(LocalDate.now());
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(17, 0));
        s.setType("WORK");
        scheduleRepo.save(s);
        return gid;
    }

    private long session(LocalTime start) {
        ScenicSession s = new ScenicSession();
        s.setTitle("派单场次");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(start);
        s.setEndTime(start.plusHours(1));
        s.setCapacity(5);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        // no fixed guide -> eligible for auto-dispatch
        return sessionRepo.save(s).getId();
    }

    @Test
    void autoDispatch_assignsAnEligibleGuide_onCreate() {
        long uid = tourist();
        eligibleGuideToday();
        long sid = session(LocalTime.of(10, 0));

        OrderView order = touristService.createOrder(uid, new CreateOrderRequest(sid, 1, "t", "13800000000", null));

        assertThat(orderRepo.findById(order.id()).orElseThrow().getGuideId()).isNotNull();
    }

    @Test
    void autoDispatch_noEligibleGuide_leavesUnassigned() {
        long uid = tourist();
        eligibleGuideToday(); // only on-duty 09:00-17:00
        long sid = session(LocalTime.of(23, 30)); // outside everyone's window

        OrderView order = touristService.createOrder(uid, new CreateOrderRequest(sid, 1, "t", "13800000000", null));

        assertThat(orderRepo.findById(order.id()).orElseThrow().getGuideId()).isNull();
    }
}
