package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/wx-phone — the dynamic code from the getPhoneNumber button. */
public record WxPhoneRequest(@NotBlank String code) {
}
