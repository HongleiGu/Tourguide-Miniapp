package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.VerifyRequest;
import com.tourguide.backend.api.dto.VerifyResult;
import com.tourguide.backend.booking.VerifyService;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 核销: a guide validates a tourist's 核销码. GUIDE-only. */
@Tag(name = "Verify")
@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final VerifyService service;

    @PostMapping
    @PreAuthorize("hasRole('GUIDE')")
    public ApiResponse<VerifyResult> verify(@AuthenticationPrincipal AuthPrincipal principal,
                                            @Valid @RequestBody VerifyRequest req) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(service.verify(principal.userId(), req.code()));
    }
}
