package com.tourguide.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands role codes into Spring Security authorities.
 *
 * <p>Each role becomes a {@code ROLE_<code>} authority (so {@code hasRole(...)} works), and admin
 * roles additionally grant fine-grained permission authorities for the 操作 / 查看 / 导出 split
 * (so {@code hasAuthority('EXPORT')} etc. works on feature endpoints).
 */
public final class RoleAuthorities {

    public static final String VIEW = "VIEW";     // 查看
    public static final String OPERATE = "OPERATE"; // 操作
    public static final String EXPORT = "EXPORT";   // 导出

    private static final Map<String, Set<String>> ADMIN_PERMISSIONS = Map.of(
            "ADMIN_SUPER", Set.of(VIEW, OPERATE, EXPORT),
            "ADMIN_OPS", Set.of(VIEW, OPERATE),
            "ADMIN_FINANCE", Set.of(VIEW, EXPORT));

    private RoleAuthorities() {
    }

    public static List<GrantedAuthority> from(Collection<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            for (String permission : ADMIN_PERMISSIONS.getOrDefault(role, Set.of())) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return authorities;
    }
}
