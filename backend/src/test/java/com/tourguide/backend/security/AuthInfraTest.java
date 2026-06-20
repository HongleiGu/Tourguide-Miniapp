package com.tourguide.backend.security;

import com.tourguide.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthInfraTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void me_withoutToken_is401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10401));
    }

    @Test
    void me_withValidToken_returnsPrincipal() throws Exception {
        String token = jwt.issueAccessToken(42L, UserType.ADMIN, List.of("ADMIN_SUPER"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.type").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN_SUPER"));
    }

    @Test
    void me_withGarbageToken_is401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withRefreshToken_is401() throws Exception {
        String refresh = jwt.issueRefreshToken(42L, UserType.ADMIN);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoint_pingStaysAccessible() throws Exception {
        mvc.perform(get("/api/ping")).andExpect(status().isOk());
    }

    @Test
    void bcrypt_encodesAndMatches() {
        String hash = encoder.encode("s3cret");
        assertThat(hash).isNotEqualTo("s3cret");
        assertThat(encoder.matches("s3cret", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }
}
