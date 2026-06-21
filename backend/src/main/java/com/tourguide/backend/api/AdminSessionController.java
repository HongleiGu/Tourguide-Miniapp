package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AdminSessionRequest;
import com.tourguide.backend.api.dto.AdminSessionView;
import com.tourguide.backend.api.dto.SessionStatusRequest;
import com.tourguide.backend.booking.AdminSessionService;
import com.tourguide.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Admin 场次管理 (MIN-48). Under /api/admin/** (ADMIN role gate). */
@Tag(name = "Admin Sessions")
@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<List<AdminSessionView>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(service.list(date));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminSessionView> create(@Valid @RequestBody AdminSessionRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminSessionView> update(@PathVariable long id,
                                                @Valid @RequestBody AdminSessionRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminSessionView> setStatus(@PathVariable long id,
                                                   @Valid @RequestBody SessionStatusRequest req) {
        return ApiResponse.ok(service.setStatus(id, req.status()));
    }
}
