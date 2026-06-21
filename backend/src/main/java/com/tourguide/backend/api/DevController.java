package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AuthTokens;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.booking.DevDataBootstrap;
import com.tourguide.backend.booking.TouristService;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.guide.GuideProfile;
import com.tourguide.backend.guide.GuideProfileRepository;
import com.tourguide.backend.security.AuthPrincipal;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import com.tourguide.backend.user.Role;
import com.tourguide.backend.user.RoleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DEV-ONLY affordances so the tourist flow is testable without real WeChat credentials:
 * a tourist login that bypasses code2session, and a mock payment. Not loaded in prod.
 */
@Tag(name = "Dev")
@Profile("dev")
@RestController
@RequiredArgsConstructor
public class DevController {

    private static final String DEV_OPENID = "dev-tourist";

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final GuideProfileRepository guideRepo;
    private final JwtService jwtService;
    private final TouristService touristService;

    @PostMapping("/api/auth/dev-login")
    @Transactional
    public ApiResponse<AuthTokens> devLogin() {
        AppUser user = userRepo.findByOpenId(DEV_OPENID).orElseGet(this::createDevTourist);
        return ApiResponse.ok(tokensFor(user));
    }

    /** Dev guide login (GUIDE token) so the guide app is testable without real WeChat. */
    @PostMapping("/api/auth/dev-login-guide")
    @Transactional
    public ApiResponse<AuthTokens> devLoginGuide() {
        AppUser user = userRepo.findByOpenId(DevDataBootstrap.DEV_GUIDE_OPENID).orElseGet(this::createDevGuide);
        guideRepo.findByUserId(user.getId()).orElseGet(() -> {
            GuideProfile p = new GuideProfile();
            p.setUserId(user.getId());
            return guideRepo.save(p);
        });
        return ApiResponse.ok(tokensFor(user));
    }

    private AuthTokens tokensFor(AppUser user) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        return new AuthTokens(
                jwtService.issueAccessToken(user.getId(), UserType.APP, roles),
                jwtService.issueRefreshToken(user.getId(), UserType.APP));
    }

    @PostMapping("/api/tourist/orders/{id}/mock-pay")
    public ApiResponse<OrderView> mockPay(@AuthenticationPrincipal AuthPrincipal principal,
                                          @PathVariable long id) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(touristService.mockPay(principal.userId(), id));
    }

    private AppUser createDevTourist() {
        AppUser user = new AppUser();
        user.setOpenId(DEV_OPENID);
        user.setNickname("Dev Tourist");
        roleRepo.findByCode("TOURIST").ifPresent(user.getRoles()::add);
        return userRepo.save(user);
    }

    private AppUser createDevGuide() {
        AppUser user = new AppUser();
        user.setOpenId(DevDataBootstrap.DEV_GUIDE_OPENID);
        user.setNickname("Dev Guide");
        roleRepo.findByCode("GUIDE").ifPresent(user.getRoles()::add);
        return userRepo.save(user);
    }
}
