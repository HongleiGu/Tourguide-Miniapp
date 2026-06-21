package com.tourguide.backend.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourguide.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TouristBookingTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void sessionsAndAnnouncements_arePublic() throws Exception {
        mvc.perform(get("/api/tourist/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].remaining").exists());
        mvc.perform(get("/api/tourist/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());
    }

    @Test
    void orders_requireAuth() throws Exception {
        mvc.perform(post("/api/tourist/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":1,\"peopleCount\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullFlow_devLogin_book_mockPay_yieldsVerifyCode() throws Exception {
        // dev tourist login
        String loginBody = mvc.perform(post("/api/auth/dev-login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(loginBody).at("/data/accessToken").asText();
        assertThat(token).isNotBlank();

        // pick a session
        String sessionsBody = mvc.perform(get("/api/tourist/sessions"))
                .andReturn().getResponse().getContentAsString();
        long sessionId = json.readTree(sessionsBody).at("/data/0/id").asLong();

        // create order
        String created = mvc.perform(post("/api/tourist/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"peopleCount\":1,\"contactName\":\"张三\",\"contactPhone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        JsonNode order = json.readTree(created).at("/data");
        long orderId = order.at("/id").asLong();
        assertThat(order.at("/verifyCode").isNull() || order.at("/verifyCode").asText().isEmpty()).isTrue();

        // mock pay -> PAID + 核销码
        mvc.perform(post("/api/tourist/orders/" + orderId + "/mock-pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.verifyCode").isNotEmpty());

        // get order reflects PAID
        mvc.perform(get("/api/tourist/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.verifyCode").isNotEmpty());

        // order center lists the user's orders
        mvc.perform(get("/api/tourist/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
    }
}
