package com.tourguide.backend.auth;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.tourguide.backend.api.dto.AuthTokens;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import com.tourguide.backend.user.Role;
import com.tourguide.backend.user.RoleRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** WeChat mini-program authentication: silent login (openid) and one-tap phone binding. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "TOURIST";

    private final WxMaService wxMaService;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /** Exchange a wx.login code for openid; create the user on first login; issue tokens. */
    @Transactional
    public AuthTokens wxLogin(String code) {
        WxMaJscode2SessionResult session;
        try {
            session = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            log.warn("wx code2session failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "微信登录失败");
        }
        AppUser user = userRepo.findByOpenId(session.getOpenid())
                .orElseGet(() -> createTourist(session.getOpenid(), session.getUnionid()));
        return issueFor(user, UserType.APP);
    }

    /** Admin login by username + bcrypt password. */
    @Transactional(readOnly = true)
    public AuthTokens adminLogin(String username, String rawPassword) {
        AppUser user = userRepo.findByUsername(username)
                .filter(u -> u.getPasswordHash() != null)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return issueFor(user, UserType.ADMIN);
    }

    /** Exchange a valid refresh token for a fresh token pair (with the user's current roles). */
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        JwtService.TokenInfo info;
        try {
            info = jwtService.introspect(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        if (!info.refresh()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        AppUser user = userRepo.findById(info.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return issueFor(user, info.type());
    }

    /** Decode the phone-number code and bind the verified phone to the current user. */
    @Transactional
    public void bindPhone(long userId, String code) {
        WxMaPhoneNumberInfo info;
        try {
            info = wxMaService.getUserService().getPhoneNoInfo(code);
        } catch (WxErrorException e) {
            log.warn("wx getPhoneNoInfo failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "获取手机号失败");
        }
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.setPhone(info.getPurePhoneNumber());
        userRepo.save(user);
    }

    private AppUser createTourist(String openid, String unionid) {
        AppUser user = new AppUser();
        user.setOpenId(openid);
        user.setUnionId(unionid);
        roleRepo.findByCode(DEFAULT_ROLE).ifPresent(user.getRoles()::add);
        return userRepo.save(user);
    }

    private AuthTokens issueFor(AppUser user, UserType type) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        String access = jwtService.issueAccessToken(user.getId(), type, roles);
        String refresh = jwtService.issueRefreshToken(user.getId(), type);
        return new AuthTokens(access, refresh);
    }
}
