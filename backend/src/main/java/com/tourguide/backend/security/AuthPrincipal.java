package com.tourguide.backend.security;

import java.util.List;

/** Authenticated principal carried in the SecurityContext, derived from a JWT. */
public record AuthPrincipal(long userId, UserType type, List<String> roles) {
}
