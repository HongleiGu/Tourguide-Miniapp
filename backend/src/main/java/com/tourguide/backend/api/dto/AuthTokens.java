package com.tourguide.backend.api.dto;

/** Access + refresh token pair returned by login/refresh. */
public record AuthTokens(String accessToken, String refreshToken) {
}
