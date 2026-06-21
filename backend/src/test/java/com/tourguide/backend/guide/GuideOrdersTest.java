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

/** GET /api/guide/orders + detail (MIN-37): own orders only. */
@AutoConfigureMockMvc
class GuideOrdersTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    GuideProfileRepository guideRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;

    private long guide(String openId) {
        AppUser u = new AppUser();
        u.setOpenId(openId);
        u.setNickname(openId);
        return userRepo.save(u).getId();
    }

    private long profile(long userId) {
        GuideProfile p = new GuideProfile();
        p.setUserId(userId);
        return guideRepo.save(p).getId();
    }

    @Test
    void listAndDetail_scopedToGuide() throws Exception {
        long uidA = guide("g-A");
        long gidA = profile(uidA);
        long uidB = guide("g-B");
        profile(uidB);

        ScenicSession s = new ScenicSession();
        s.setTitle("讲解场次A");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 0));
        s.setCapacity(5);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        s.setGuideId(gidA);
        long sid = sessionRepo.save(s).getId();

        BookingOrder o = new BookingOrder();
        o.setOrderNo("GO1");
        o.setUserId(1L);
        o.setSessionId(sid);
        o.setGuideId(gidA);
        o.setType("PRIVATE");
        o.setPeopleCount(2);
        o.setContactName("王游客");
        o.setContactPhone("13900000000");
        o.setVisitDate(LocalDate.now());
        o.setAmountFen(2000L);
        o.setStatus("PAID");
        long oid = orderRepo.save(o).getId();

        String tokenA = jwt.issueAccessToken(uidA, UserType.APP, List.of("GUIDE"));
        String tokenB = jwt.issueAccessToken(uidB, UserType.APP, List.of("GUIDE"));

        mvc.perform(get("/api/guide/orders").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].contactName").value("王游客"));

        mvc.perform(get("/api/guide/orders/" + oid).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactPhone").value("13900000000"));

        // another guide cannot see it
        mvc.perform(get("/api/guide/orders/" + oid).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // B's own list is empty
        mvc.perform(get("/api/guide/orders").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
