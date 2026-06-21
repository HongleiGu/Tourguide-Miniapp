package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AdminUserView;
import com.tourguide.backend.api.dto.CreateAdminRequest;
import com.tourguide.backend.api.dto.RolesRequest;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.user.AdminUserService;
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

/** 管理员账号 + 权限分配 (MIN-52). Super-admin only. */
@Tag(name = "Admin Users")
@RestController
@RequestMapping("/api/admin/admins")
@PreAuthorize("hasRole('ADMIN_SUPER')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public ApiResponse<List<AdminUserView>> list() {
        return ApiResponse.ok(service.listAdmins());
    }

    @PostMapping
    public ApiResponse<AdminUserView> create(@Valid @RequestBody CreateAdminRequest req) {
        return ApiResponse.ok(service.create(req.username(), req.password(), req.roles()));
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<AdminUserView> setRoles(@PathVariable long id,
                                               @Valid @RequestBody RolesRequest req) {
        return ApiResponse.ok(service.setRoles(id, req.roles()));
    }
}
