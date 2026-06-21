package com.tourguide.backend.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 人员管理 (MIN-47): create / enable-disable / employment. */
@AutoConfigureMockMvc
class AdminGuideMgmtTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    ObjectMapper json;

    @Test
    void create_list_enable_employment() throws Exception {
        String ops = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS"));

        String created = mvc.perform(post("/api/admin/guides")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新讲解员\",\"employmentType\":\"OUTSOURCED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新讲解员"))
                .andExpect(jsonPath("$.data.employmentType").value("OUTSOURCED"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andReturn().getResponse().getContentAsString();
        long gid = json.readTree(created).at("/data/guideId").asLong();

        mvc.perform(get("/api/admin/guides").header("Authorization", "Bearer " + ops))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.guideId == " + gid + ")]").exists());

        mvc.perform(post("/api/admin/guides/" + gid + "/enabled")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mvc.perform(post("/api/admin/guides/" + gid + "/employment")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"employmentType\":\"SELF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentType").value("SELF"));
    }

    @Test
    void create_requiresOperate() throws Exception {
        String finance = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_FINANCE"));
        mvc.perform(post("/api/admin/guides").header("Authorization", "Bearer " + finance)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
