package com.tourguide.backend.security;

import com.tourguide.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the /api/admin/** role gate and the 操作/查看/导出 permission gates. */
@AutoConfigureMockMvc
class RbacTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    private String token(String... roles) {
        return jwt.issueAccessToken(1L, UserType.ADMIN, List.of(roles));
    }

    private String appToken(String... roles) {
        return jwt.issueAccessToken(2L, UserType.APP, List.of(roles));
    }

    @Test
    void adminNamespace_requiresAuth() throws Exception {
        mvc.perform(get("/api/admin/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminNamespace_forbiddenForNonAdmin() throws Exception {
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + appToken("TOURIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdmin_hasEverything() throws Exception {
        String t = token("ADMIN_SUPER");
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/export").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/operate").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
    }

    @Test
    void finance_canExport_butNotOperate() throws Exception {
        String t = token("ADMIN_FINANCE");
        mvc.perform(get("/api/admin/export").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/operate").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test
    void ops_canOperate_butNotExport() throws Exception {
        String t = token("ADMIN_OPS");
        mvc.perform(post("/api/admin/operate").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/export").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestControllers {
        @Bean
        TestAdminApi testAdminApi() {
            return new TestAdminApi();
        }
    }

    @RestController
    @RequestMapping("/api/admin")
    static class TestAdminApi {

        @GetMapping("/probe")
        String probe() {
            return "ok";
        }

        @GetMapping("/export")
        @PreAuthorize("hasAuthority('EXPORT')")
        String export() {
            return "ok";
        }

        @PostMapping("/operate")
        @PreAuthorize("hasAuthority('OPERATE')")
        String operate() {
            return "ok";
        }
    }
}
