package com.tourguide.backend.api.dto;

import java.util.List;

/** Sample typed payload for {@code GET /api/ping}; gives the generated TS types a named schema. */
public record PingResponse(String service, List<String> profiles, String time) {
}
