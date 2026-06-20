package com.tourguide.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUserRepository;
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
class AdminAuthTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppUserRepository userRepo;

    @Autowired
    JwtService jwt;

    @Autowired
    ObjectMapper json;

    @Test
    void bootstrap_createdDevSuperAdmin() {
        assertThat(userRepo.findByUsername("admin")).isPresent();
    }

    @Test
    void adminLogin_succeeds_andMeReportsAdminRole() throws Exception {
        String body = login("admin", "admin123").andReturn().getResponse().getContentAsString();
        String access = json.readTree(body).at("/data/accessToken").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN_SUPER"));
    }

    @Test
    void adminLogin_wrongPassword_is401() throws Exception {
        mvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminLogin_unknownUser_is401() throws Exception {
        mvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_issuesUsableAccessToken() throws Exception {
        String body = login("admin", "admin123").andReturn().getResponse().getContentAsString();
        String refresh = json.readTree(body).at("/data/refreshToken").asText();

        String refreshed = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshBody(refresh))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(refreshed).at("/data");
        String newAccess = data.at("/accessToken").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("ADMIN"));
    }

    @Test
    void refresh_withAccessToken_is401() throws Exception {
        String access = jwt.issueAccessToken(1L, UserType.ADMIN, java.util.List.of("ADMIN_SUPER"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshBody(access))))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(String u, String p) throws Exception {
        return mvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}"))
                .andExpect(status().isOk());
    }

    private record RefreshBody(String refreshToken) {
    }
}
