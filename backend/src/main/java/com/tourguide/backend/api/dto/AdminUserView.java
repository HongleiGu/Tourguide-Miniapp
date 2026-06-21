package com.tourguide.backend.api.dto;

import java.util.List;

/** Admin account (with assigned roles). */
public record AdminUserView(
        Long id,
        String username,
        String nickname,
        List<String> roles) {
}
