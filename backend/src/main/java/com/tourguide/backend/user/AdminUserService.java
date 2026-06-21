package com.tourguide.backend.user;

import com.tourguide.backend.api.dto.AdminUserView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 管理员账号 + 权限分配 (MIN-52). Super-admin only (gated at the controller). */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUserView> listAdmins() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode().startsWith("ADMIN_")))
                .map(this::view)
                .toList();
    }

    @Transactional
    public AdminUserView create(String username, String password, List<String> roleCodes) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRoles(resolveRoles(roleCodes));
        return view(userRepo.save(u));
    }

    @Transactional
    public AdminUserView setRoles(long userId, List<String> roleCodes) {
        AppUser u = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账号不存在"));
        u.setRoles(resolveRoles(roleCodes));
        return view(userRepo.save(u));
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            roles.add(roleRepo.findByCode(code)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "未知角色: " + code)));
        }
        return roles;
    }

    private AdminUserView view(AppUser u) {
        return new AdminUserView(u.getId(), u.getUsername(), u.getNickname(),
                u.getRoles().stream().map(Role::getCode).sorted().toList());
    }
}
