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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/guide/income (MIN-40): gross PAID/COMPLETED totals. */
@AutoConfigureMockMvc
class GuideIncomeTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    GuideProfileRepository guideRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;

    private long userId;
    private long sessionId;

    private void order(long guideId, String no, long fen, String statusValue) {
        BookingOrder o = new BookingOrder();
        o.setOrderNo(no);
        o.setUserId(userId);
        o.setSessionId(sessionId);
        o.setGuideId(guideId);
        o.setType("PRIVATE");
        o.setPeopleCount(1);
        o.setVisitDate(LocalDate.now());
        o.setAmountFen(fen);
        o.setStatus(statusValue);
        orderRepo.save(o);
    }

    @Test
    void income_sumsPaidAndCompletedOnly() throws Exception {
        AppUser u = new AppUser();
        u.setNickname("收入讲解员");
        long uid = userRepo.save(u).getId();
        this.userId = uid;
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
        long gid = guideRepo.save(p).getId();

        ScenicSession s = new ScenicSession();
        s.setTitle("收入场次");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setCapacity(5);
        s.setPriceFen(10000L);
        s.setStatus("OPEN");
        s.setGuideId(gid);
        this.sessionId = sessionRepo.save(s).getId();

        order(gid, "IN1", 10000L, "PAID");
        order(gid, "IN2", 5000L, "COMPLETED");
        order(gid, "IN3", 7000L, "CANCELLED"); // excluded
        order(gid, "IN4", 3000L, "PENDING_PAYMENT"); // excluded

        String token = jwt.issueAccessToken(uid, UserType.APP, List.of("GUIDE"));
        mvc.perform(get("/api/guide/income").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(2))
                .andExpect(jsonPath("$.data.totalFen").value(15000));
    }
}
