package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.GuideMe;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.guide.GuideService;
import com.tourguide.backend.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 讲解员工作台 (MIN-6). GUIDE-only. */
@Tag(name = "Guide")
@RestController
@RequestMapping("/api/guide")
@PreAuthorize("hasRole('GUIDE')")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService service;

    @GetMapping("/me")
    public ApiResponse<GuideMe> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.me(requireUser(principal)));
    }

    private long requireUser(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
