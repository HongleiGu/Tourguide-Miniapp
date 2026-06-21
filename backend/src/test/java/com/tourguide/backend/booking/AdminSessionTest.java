package com.tourguide.backend.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin 场次管理 (MIN-48). */
@AutoConfigureMockMvc
class AdminSessionTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    ObjectMapper json;

    @Test
    void createGroupSession_setsGroupBuy_thenLock() throws Exception {
        String ops = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS"));
        String body = "{\"title\":\"管理拼团场\",\"type\":\"GROUP\",\"date\":\"2026-12-01\","
                + "\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"priceFen\":8000,"
                + "\"groupMinSize\":2,\"groupMaxSize\":8}";

        String created = mvc.perform(post("/api/admin/sessions")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andExpect(jsonPath("$.data.capacity").value(8))
                .andExpect(jsonPath("$.data.groupMin").value(2))
                .andExpect(jsonPath("$.data.groupMax").value(8))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).at("/data/id").asLong();

        mvc.perform(get("/api/admin/sessions").param("date", "2026-12-01")
                        .header("Authorization", "Bearer " + ops))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mvc.perform(post("/api/admin/sessions/" + id + "/status")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void create_requiresOperate() throws Exception {
        String finance = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_FINANCE"));
        mvc.perform(post("/api/admin/sessions").header("Authorization", "Bearer " + finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"type\":\"PRIVATE\",\"date\":\"2026-12-01\",\"priceFen\":100}"))
                .andExpect(status().isForbidden());
    }
}
