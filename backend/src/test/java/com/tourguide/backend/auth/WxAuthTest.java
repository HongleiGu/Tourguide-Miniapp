package com.tourguide.backend.auth;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WxAuthTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppUserRepository userRepo;

    @Autowired
    JwtService jwt;

    @MockitoBean
    WxMaService wxMaService;

    final WxMaUserService wxUserService = mock(WxMaUserService.class);

    @BeforeEach
    void wireMaUserService() {
        when(wxMaService.getUserService()).thenReturn(wxUserService);
    }

    @Test
    void wxLogin_createsTouristAndIssuesTokens() throws Exception {
        WxMaJscode2SessionResult session = new WxMaJscode2SessionResult();
        session.setOpenid("openid-001");
        when(wxUserService.getSessionInfo("code-1")).thenReturn(session);

        mvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        AppUser user = userRepo.findByOpenId("openid-001").orElseThrow();
        assertThat(user.getRoles()).anyMatch(r -> r.getCode().equals("TOURIST"));
    }

    @Test
    void wxLogin_existingUser_isReusedNotDuplicated() throws Exception {
        WxMaJscode2SessionResult session = new WxMaJscode2SessionResult();
        session.setOpenid("openid-002");
        when(wxUserService.getSessionInfo(anyString())).thenReturn(session);

        mvc.perform(post("/api/auth/wx-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"a\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/wx-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"b\"}")).andExpect(status().isOk());

        long count = userRepo.findAll().stream()
                .filter(u -> "openid-002".equals(u.getOpenId())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void wxPhone_bindsPhoneToCurrentUser() throws Exception {
        WxMaJscode2SessionResult session = new WxMaJscode2SessionResult();
        session.setOpenid("openid-003");
        when(wxUserService.getSessionInfo("c")).thenReturn(session);
        mvc.perform(post("/api/auth/wx-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c\"}")).andExpect(status().isOk());
        AppUser user = userRepo.findByOpenId("openid-003").orElseThrow();

        WxMaPhoneNumberInfo phoneInfo = new WxMaPhoneNumberInfo();
        phoneInfo.setPurePhoneNumber("13800138000");
        when(wxUserService.getPhoneNoInfo("phone-code")).thenReturn(phoneInfo);

        String token = jwt.issueAccessToken(user.getId(), UserType.APP, List.of("TOURIST"));
        mvc.perform(post("/api/auth/wx-phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"phone-code\"}"))
                .andExpect(status().isOk());

        assertThat(userRepo.findById(user.getId()).orElseThrow().getPhone()).isEqualTo("13800138000");
    }

    @Test
    void wxPhone_withoutAuth_is401() throws Exception {
        mvc.perform(post("/api/auth/wx-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
