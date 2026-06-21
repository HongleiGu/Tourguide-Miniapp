package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AdminGuideView;
import com.tourguide.backend.api.dto.CreateGuideRequest;
import com.tourguide.backend.api.dto.EmploymentRequest;
import com.tourguide.backend.api.dto.EnabledRequest;
import com.tourguide.backend.api.dto.SuspendRequest;
import com.tourguide.backend.api.dto.WeightRequest;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.guide.GuideService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin guide controls (MIN-43): 派单权重 + 暂停接单. Under /api/admin/** (ADMIN role gate) and
 * requires the OPERATE permission. Fuller people-management UI is MIN-8.
 */
@Tag(name = "Admin Guides")
@RestController
@RequestMapping("/api/admin/guides")
@RequiredArgsConstructor
public class AdminGuideController {

    private final GuideService service;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<List<AdminGuideView>> list() {
        return ApiResponse.ok(service.listGuides());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminGuideView> create(@Valid @RequestBody CreateGuideRequest req) {
        return ApiResponse.ok(service.createGuide(req.name(), req.employmentType()));
    }

    @PostMapping("/{id}/enabled")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminGuideView> setEnabled(@PathVariable long id,
                                                  @Valid @RequestBody EnabledRequest req) {
        return ApiResponse.ok(service.setEnabled(id, req.enabled()));
    }

    @PostMapping("/{id}/employment")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminGuideView> setEmployment(@PathVariable long id,
                                                     @Valid @RequestBody EmploymentRequest req) {
        return ApiResponse.ok(service.setEmployment(id, req.employmentType()));
    }

    @PostMapping("/{id}/dispatch-weight")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminGuideView> setWeight(@PathVariable long id,
                                                 @Valid @RequestBody WeightRequest req) {
        return ApiResponse.ok(service.setDispatchWeight(id, req.weight()));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminGuideView> suspend(@PathVariable long id,
                                               @Valid @RequestBody SuspendRequest req) {
        return ApiResponse.ok(service.setSuspended(id, req.suspended()));
    }
}
