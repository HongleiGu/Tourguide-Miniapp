package com.tourguide.backend.user;

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

/** 管理员账号 + 权限分配 (MIN-52): super-admin only. */
@AutoConfigureMockMvc
class AdminUserMgmtTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    ObjectMapper json;

    @Test
    void create_list_assignRoles() throws Exception {
        String su = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_SUPER"));

        String created = mvc.perform(post("/api/admin/admins")
                        .header("Authorization", "Bearer " + su)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mgmt-ops-1\",\"password\":\"pw123456\",\"roles\":[\"ADMIN_OPS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("mgmt-ops-1"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN_OPS"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).at("/data/id").asLong();

        mvc.perform(get("/api/admin/admins").header("Authorization", "Bearer " + su))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mvc.perform(post("/api/admin/admins/" + id + "/roles")
                        .header("Authorization", "Bearer " + su)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ADMIN_FINANCE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN_FINANCE"));
    }

    @Test
    void nonSuperAdmin_cannotManage() throws Exception {
        String ops = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS"));
        mvc.perform(post("/api/admin/admins").header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"y123456\",\"roles\":[\"ADMIN_OPS\"]}"))
                .andExpect(status().isForbidden());
    }
}
