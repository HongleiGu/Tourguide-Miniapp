package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AcceptingRequest;
import com.tourguide.backend.api.dto.GuideIncome;
import com.tourguide.backend.api.dto.GuideMe;
import com.tourguide.backend.api.dto.GuideOrderView;
import com.tourguide.backend.api.dto.GuideWorkbench;
import com.tourguide.backend.api.dto.ScheduleSegment;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.guide.GuideService;
import com.tourguide.backend.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/workbench")
    public ApiResponse<GuideWorkbench> workbench(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.workbench(requireUser(principal)));
    }

    @GetMapping("/orders")
    public ApiResponse<List<GuideOrderView>> orders(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.listOrders(requireUser(principal), status));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<GuideOrderView> order(@AuthenticationPrincipal AuthPrincipal principal,
                                             @PathVariable long id) {
        return ApiResponse.ok(service.getOrder(requireUser(principal), id));
    }

    @PostMapping("/accepting")
    public ApiResponse<GuideMe> setAccepting(@AuthenticationPrincipal AuthPrincipal principal,
                                             @Valid @RequestBody AcceptingRequest req) {
        return ApiResponse.ok(service.setAccepting(requireUser(principal), req.accepting()));
    }

    @GetMapping("/income")
    public ApiResponse<GuideIncome> income(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.income(requireUser(principal)));
    }

    @GetMapping("/schedule")
    public ApiResponse<List<ScheduleSegment>> schedule(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);
        return ApiResponse.ok(service.schedule(requireUser(principal), start, end));
    }

    private long requireUser(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
