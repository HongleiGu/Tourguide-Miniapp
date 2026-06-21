package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.PricingRuleRequest;
import com.tourguide.backend.api.dto.PricingRuleView;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.pricing.PricingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin 价格与拼团规则 (MIN-49). */
@Tag(name = "Admin Pricing")
@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
public class AdminPricingController {

    private final PricingService service;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<List<PricingRuleView>> list() {
        return ApiResponse.ok(service.list());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<PricingRuleView> upsert(@Valid @RequestBody PricingRuleRequest req) {
        return ApiResponse.ok(service.upsert(req));
    }
}
