package com.tourguide.backend.pricing;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 价格与拼团规则 (MIN-49): config CRUD + session price defaults from rule. */
@AutoConfigureMockMvc
class PricingTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;

    private String ops() {
        return jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS"));
    }

    private void upsert(String token, String type, String dayType, long price) throws Exception {
        mvc.perform(put("/api/admin/pricing").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionType\":\"" + type + "\",\"dayType\":\"" + dayType
                                + "\",\"priceFen\":" + price + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void list_upsert_andSessionDefaultsFromRule() throws Exception {
        String ops = ops();

        mvc.perform(get("/api/admin/pricing").header("Authorization", "Bearer " + ops))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionType").exists());

        // make PRIVATE the same price on workday + holiday so the assertion is date-independent
        upsert(ops, "PRIVATE", "WORKDAY", 50000);
        upsert(ops, "PRIVATE", "HOLIDAY", 50000);

        // creating a PRIVATE session with no price falls back to the rule
        mvc.perform(post("/api/admin/sessions").header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"默认价场\",\"type\":\"PRIVATE\",\"date\":\"2026-12-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priceFen").value(50000));
    }

    @Test
    void upsert_requiresOperate() throws Exception {
        String finance = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_FINANCE"));
        mvc.perform(put("/api/admin/pricing").header("Authorization", "Bearer " + finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionType\":\"PRIVATE\",\"dayType\":\"WORKDAY\",\"priceFen\":1}"))
                .andExpect(status().isForbidden());
    }
}
