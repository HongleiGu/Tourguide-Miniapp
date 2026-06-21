package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 基础统计 (MIN-51). Uses a far-future date so only this test's orders fall in range. */
@AutoConfigureMockMvc
class StatsTest extends AbstractIntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2030, 6, 15);

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;

    private void seed() {
        AppUser u = new AppUser();
        u.setNickname("统计游客");
        long uid = userRepo.save(u).getId();
        ScenicSession s = new ScenicSession();
        s.setTitle("统计场次");
        s.setType("PRIVATE");
        s.setSessionDate(DAY);
        s.setCapacity(10);
        s.setPriceFen(5000L);
        s.setStatus("OPEN");
        long sid = sessionRepo.save(s).getId();
        order(uid, sid, "ST-PAID", 2, 10000L, "PAID");
        order(uid, sid, "ST-DONE", 1, 5000L, "COMPLETED");
    }

    private void order(long uid, long sid, String no, int people, long fen, String statusValue) {
        BookingOrder o = new BookingOrder();
        o.setOrderNo(no);
        o.setUserId(uid);
        o.setSessionId(sid);
        o.setType("PRIVATE");
        o.setPeopleCount(people);
        o.setVisitDate(DAY);
        o.setAmountFen(fen);
        o.setStatus(statusValue);
        orderRepo.save(o);
    }

    @Test
    void stats_andExport() throws Exception {
        seed();
        String su = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_SUPER"));

        mvc.perform(get("/api/admin/stats")
                        .param("from", DAY.toString()).param("to", DAY.toString())
                        .header("Authorization", "Bearer " + su))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.paidOrders").value(1))
                .andExpect(jsonPath("$.data.completedOrders").value(1))
                .andExpect(jsonPath("$.data.visitors").value(3))
                .andExpect(jsonPath("$.data.revenueFen").value(15000))
                .andExpect(jsonPath("$.data.verificationRate").value(0.5));

        byte[] xlsx = mvc.perform(get("/api/admin/stats/export")
                        .param("from", DAY.toString()).param("to", DAY.toString())
                        .header("Authorization", "Bearer " + su))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(xlsx.length).isGreaterThan(0);
        assertThat(xlsx[0]).isEqualTo((byte) 'P');
    }
}
