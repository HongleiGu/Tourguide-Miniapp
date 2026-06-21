package com.tourguide.backend.guide;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 接单状态 toggle + 排班 + off-hours eligibility (MIN-39). */
@AutoConfigureMockMvc
class AcceptingScheduleTest extends AbstractIntegrationTest {

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
    GuideService guideService;

    private long[] seedGuideWithWorkDay() {
        AppUser u = new AppUser();
        u.setNickname("排班讲解员");
        long uid = userRepo.save(u).getId();
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
        long gid = guideRepo.save(p).getId();

        GuideSchedule work = new GuideSchedule();
        work.setGuideId(gid);
        work.setWorkDate(LocalDate.now());
        work.setStartTime(LocalTime.of(9, 0));
        work.setEndTime(LocalTime.of(17, 0));
        work.setType("WORK");
        scheduleRepo.save(work);
        return new long[]{uid, gid};
    }

    @Test
    void eligibility_respectsScheduleAndAccepting() {
        long[] ids = seedGuideWithWorkDay();
        long uid = ids[0];
        long gid = ids[1];

        assertThat(guideService.isAcceptingAt(gid, LocalDate.now(), LocalTime.of(10, 0))).isTrue();
        // outside the WORK window
        assertThat(guideService.isAcceptingAt(gid, LocalDate.now(), LocalTime.of(20, 0))).isFalse();

        // turning off accepting blocks even within hours
        guideService.setAccepting(uid, false);
        assertThat(guideService.isAcceptingAt(gid, LocalDate.now(), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void acceptingToggle_andScheduleEndpoints() throws Exception {
        long[] ids = seedGuideWithWorkDay();
        String token = jwt.issueAccessToken(ids[0], UserType.APP, List.of("GUIDE"));

        mvc.perform(post("/api/guide/accepting").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accepting\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptingOrders").value(false));

        mvc.perform(get("/api/guide/schedule").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("WORK"));
    }
}
