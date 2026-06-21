package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 核销 (MIN-33): GUIDE-only, single-use, traceable. */
@AutoConfigureMockMvc
class VerifyTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;
    @Autowired
    TouristService touristService;

    private String guideToken() {
        return jwt.issueAccessToken(99L, UserType.APP, List.of("GUIDE"));
    }

    private String paidVerifyCode() {
        ScenicSession s = new ScenicSession();
        s.setTitle("核销测试");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 0));
        s.setCapacity(1);
        s.setPriceFen(1000L);
        s.setStatus("OPEN");
        long sessionId = sessionRepo.save(s).getId();
        OrderView order = touristService.createOrder(2L, new CreateOrderRequest(sessionId, 1, "t", "13800000000", null));
        OrderView paid = touristService.mockPay(2L, order.id());
        return paid.verifyCode();
    }

    private String body(String code) {
        return "{\"code\":\"" + code + "\"}";
    }

    @Test
    void verify_completesOrder_thenRejectsReuse() throws Exception {
        String code = paidVerifyCode();
        String guide = guideToken();

        mvc.perform(post("/api/verify").header("Authorization", "Bearer " + guide)
                        .contentType(MediaType.APPLICATION_JSON).content(body(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 不可重复核销
        mvc.perform(post("/api/verify").header("Authorization", "Bearer " + guide)
                        .contentType(MediaType.APPLICATION_JSON).content(body(code)))
                .andExpect(status().isConflict());
    }

    @Test
    void verify_invalidCode_notFound() throws Exception {
        mvc.perform(post("/api/verify").header("Authorization", "Bearer " + guideToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body("NOPE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void verify_requiresGuideRole() throws Exception {
        String tourist = jwt.issueAccessToken(2L, UserType.APP, List.of("TOURIST"));
        mvc.perform(post("/api/verify").header("Authorization", "Bearer " + tourist)
                        .contentType(MediaType.APPLICATION_JSON).content(body("ANY")))
                .andExpect(status().isForbidden());
    }
}
