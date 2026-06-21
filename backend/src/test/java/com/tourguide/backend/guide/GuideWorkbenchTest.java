package com.tourguide.backend.guide;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.booking.BookingOrder;
import com.tourguide.backend.booking.BookingOrderRepository;
import com.tourguide.backend.booking.ScenicSession;
import com.tourguide.backend.booking.ScenicSessionRepository;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/guide/workbench (MIN-36). */
@AutoConfigureMockMvc
class GuideWorkbenchTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
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

    private void order(long sessionId, long guideId, String no, int people, String statusValue) {
        BookingOrder o = new BookingOrder();
        o.setOrderNo(no);
        o.setUserId(1L);
        o.setSessionId(sessionId);
        o.setGuideId(guideId);
        o.setType("PRIVATE");
        o.setPeopleCount(people);
        o.setVisitDate(LocalDate.now());
        o.setAmountFen(1000L);
        o.setStatus(statusValue);
        orderRepo.save(o);
    }

    @Test
    void workbench_countsScheduleAndRemaining() throws Exception {
        AppUser u = new AppUser();
        u.setNickname("讲解员李");
        long uid = userRepo.save(u).getId();
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
        long gid = guideRepo.save(p).getId();

        ScenicSession s = new ScenicSession();
        s.setTitle("今日讲解");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setCapacity(10);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        s.setGuideId(gid);
        long sid = sessionRepo.save(s).getId();

        order(sid, gid, "WB1", 2, "PAID");
        order(sid, gid, "WB2", 1, "COMPLETED");
        order(sid, gid, "WB3", 1, "PENDING_PAYMENT");

        GuideSchedule sch = new GuideSchedule();
        sch.setGuideId(gid);
        sch.setWorkDate(LocalDate.now());
        sch.setStartTime(LocalTime.of(9, 0));
        sch.setEndTime(LocalTime.of(17, 0));
        sch.setType("WORK");
        scheduleRepo.save(sch);

        String token = jwt.issueAccessToken(uid, UserType.APP, List.of("GUIDE"));
        mvc.perform(get("/api/guide/workbench").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingAcceptCount").value(1))
                .andExpect(jsonPath("$.data.toVerifyCount").value(1))
                .andExpect(jsonPath("$.data.completedCount").value(1))
                .andExpect(jsonPath("$.data.onDutyToday").value(true))
                .andExpect(jsonPath("$.data.remainingCapacity").value(7))
                .andExpect(jsonPath("$.data.schedule[0].type").value("WORK"));
    }
}
