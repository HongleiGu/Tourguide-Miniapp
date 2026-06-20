package com.tourguide.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/wx-login — the code from wx.login. */
public record WxLoginRequest(@NotBlank String code) {
}
