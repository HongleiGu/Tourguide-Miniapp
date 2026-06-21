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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin 派单权重 + 暂停接单 (MIN-43): OPERATE-gated; suspend removes dispatch eligibility. */
@AutoConfigureMockMvc
class AdminGuideTest extends AbstractIntegrationTest {

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

    private long guideOnDuty() {
        AppUser u = new AppUser();
        u.setNickname("被管理讲解员");
        long uid = userRepo.save(u).getId();
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
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

    @Test
    void ops_setsWeight_andSuspend_removesEligibility() throws Exception {
        long gid = guideOnDuty();
        String ops = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS"));

        assertThat(guideService.isAcceptingAt(gid, LocalDate.now(), LocalTime.of(10, 0))).isTrue();

        mvc.perform(post("/api/admin/guides/" + gid + "/dispatch-weight")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"weight\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dispatchWeight").value(50));

        mvc.perform(post("/api/admin/guides/" + gid + "/suspend")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"suspended\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // suspended => no longer eligible for dispatch/grab
        assertThat(guideService.isAcceptingAt(gid, LocalDate.now(), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void finance_cannotOperate() throws Exception {
        long gid = guideOnDuty();
        String finance = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_FINANCE"));
        mvc.perform(post("/api/admin/guides/" + gid + "/dispatch-weight")
                        .header("Authorization", "Bearer " + finance)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"weight\":10}"))
                .andExpect(status().isForbidden());
    }
}
