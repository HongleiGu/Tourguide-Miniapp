package com.tourguide.backend.guide;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.booking.BookingOrder;
import com.tourguide.backend.booking.BookingOrderRepository;
import com.tourguide.backend.booking.ScenicSession;
import com.tourguide.backend.booking.ScenicSessionRepository;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 手动抢单 (MIN-42): guarded claim => single winner; pool scoped to eligibility. */
class GrabTest extends AbstractIntegrationTest {

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
    GuideService guideService;

    private long[] eligibleGuide() {
        AppUser u = new AppUser();
        u.setNickname("抢单讲解员");
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
        return new long[]{uid, gid};
    }

    private long unassignedOrder() {
        ScenicSession s = new ScenicSession();
        s.setTitle("抢单场次");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(10, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setCapacity(5);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        long sid = sessionRepo.save(s).getId();

        AppUser t = new AppUser();
        t.setNickname("抢单游客");
        long uid = userRepo.save(t).getId();

        BookingOrder o = new BookingOrder();
        o.setOrderNo("GRAB-" + sid);
        o.setUserId(uid);
        o.setSessionId(sid);
        o.setGuideId(null);
        o.setType("PRIVATE");
        o.setPeopleCount(1);
        o.setVisitDate(LocalDate.now());
        o.setAmountFen(1000L);
        o.setStatus("PAID");
        return orderRepo.save(o).getId();
    }

    @Test
    void grab_singleWinner_thenConflict() {
        long[] a = eligibleGuide();
        long[] b = eligibleGuide();
        long oid = unassignedOrder();

        // both see it in their pool
        assertThat(guideService.pool(a[0])).anyMatch(v -> v.id() == oid);
        assertThat(guideService.pool(b[0])).anyMatch(v -> v.id() == oid);

        guideService.grab(a[0], oid);
        assertThat(orderRepo.findById(oid).orElseThrow().getGuideId()).isEqualTo(a[1]);

        // second grab loses
        assertThatThrownBy(() -> guideService.grab(b[0], oid))
                .isInstanceOf(BusinessException.class);

        // and it's no longer in the pool
        assertThat(guideService.pool(b[0])).noneMatch(v -> v.id() == oid);
    }
}
