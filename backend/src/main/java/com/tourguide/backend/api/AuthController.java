package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AdminLoginRequest;
import com.tourguide.backend.api.dto.AuthTokens;
import com.tourguide.backend.api.dto.MeResponse;
import com.tourguide.backend.api.dto.RefreshRequest;
import com.tourguide.backend.api.dto.WxLoginRequest;
import com.tourguide.backend.api.dto.WxPhoneRequest;
import com.tourguide.backend.auth.AuthService;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. Admin login/refresh land in MIN-19. */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Admin login by username + password. */
    @PostMapping("/admin/login")
    public ApiResponse<AuthTokens> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return ApiResponse.ok(authService.adminLogin(req.username(), req.password()));
    }

    /** Exchange a refresh token for a fresh token pair. */
    @PostMapping("/refresh")
    public ApiResponse<AuthTokens> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(authService.refresh(req.refreshToken()));
    }

    /** WeChat silent login: exchange a wx.login code for tokens. */
    @PostMapping("/wx-login")
    public ApiResponse<AuthTokens> wxLogin(@Valid @RequestBody WxLoginRequest req) {
        return ApiResponse.ok(authService.wxLogin(req.code()));
    }

    /** Bind the WeChat-verified phone number to the current user (one-tap). Requires auth. */
    @PostMapping("/wx-phone")
    public ApiResponse<Void> wxPhone(@AuthenticationPrincipal AuthPrincipal principal,
                                     @Valid @RequestBody WxPhoneRequest req) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        authService.bindPhone(principal.userId(), req.code());
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(new MeResponse(principal.userId(), principal.type().name(), principal.roles()));
    }
}
