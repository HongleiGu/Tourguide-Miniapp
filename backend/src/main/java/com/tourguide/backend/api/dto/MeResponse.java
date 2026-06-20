package com.tourguide.backend.api.dto;

import java.util.List;

/** Current authenticated principal, returned by GET /api/auth/me. */
public record MeResponse(long userId, String type, List<String> roles) {
}
