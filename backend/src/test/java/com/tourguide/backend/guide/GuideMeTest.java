package com.tourguide.backend.guide;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/guide/me (MIN-35): GUIDE-only profile lookup. */
@AutoConfigureMockMvc
class GuideMeTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    GuideProfileRepository guideRepo;

    @Test
    void me_returnsProfile() throws Exception {
        AppUser u = new AppUser();
        u.setNickname("讲解员张");
        long uid = userRepo.save(u).getId();
        GuideProfile p = new GuideProfile();
        p.setUserId(uid);
        guideRepo.save(p);

        String token = jwt.issueAccessToken(uid, UserType.APP, List.of("GUIDE"));
        mvc.perform(get("/api/guide/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("讲解员张"))
                .andExpect(jsonPath("$.data.acceptingOrders").value(true))
                .andExpect(jsonPath("$.data.employmentType").value("SELF"));
    }

    @Test
    void me_forbiddenForTourist() throws Exception {
        String token = jwt.issueAccessToken(2L, UserType.APP, List.of("TOURIST"));
        mvc.perform(get("/api/guide/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
