package com.tourguide.backend.auth;

import com.tourguide.backend.config.AppProperties;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import com.tourguide.backend.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ensures an initial super-admin exists on startup, from app.admin.username/password
 * (env ADMIN_USERNAME / ADMIN_PASSWORD). Idempotent; skipped when not configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final String SUPER_ADMIN_ROLE = "ADMIN_SUPER";

    private final AppProperties props;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || !StringUtils.hasText(admin.username()) || !StringUtils.hasText(admin.password())) {
            log.warn("Admin bootstrap skipped: app.admin.username/password not configured");
            return;
        }
        if (userRepo.findByUsername(admin.username()).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(admin.username());
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        roleRepo.findByCode(SUPER_ADMIN_ROLE).ifPresent(user.getRoles()::add);
        userRepo.save(user);
        log.info("Bootstrapped super-admin '{}'", admin.username());
    }
}
