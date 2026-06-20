package com.tourguide.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header and populates the SecurityContext.
 * Invalid tokens leave the request unauthenticated (the entry point then returns 401).
 * Refresh tokens are rejected here — they are only usable at the refresh endpoint.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            try {
                Claims claims = jwtService.parse(header.substring(PREFIX.length()));
                if (!jwtService.isRefreshToken(claims)) {
                    SecurityContextHolder.getContext().setAuthentication(toAuthentication(claims, request));
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken toAuthentication(Claims claims, HttpServletRequest request) {
        long userId = Long.parseLong(claims.getSubject());
        UserType type = UserType.valueOf(claims.get(JwtService.CLAIM_TYPE, String.class));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
        if (roles == null) {
            roles = List.of();
        }
        var principal = new AuthPrincipal(userId, type, roles);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, RoleAuthorities.from(roles));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }
}
