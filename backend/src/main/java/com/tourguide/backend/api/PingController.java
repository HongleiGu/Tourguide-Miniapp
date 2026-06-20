package com.tourguide.backend.api;

import com.tourguide.backend.common.ApiResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Sample endpoint demonstrating the standard {@link ApiResponse} envelope. */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    private final Environment env;

    public PingController(Environment env) {
        this.env = env;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "service", "tourguide-backend",
                "profiles", List.of(env.getActiveProfiles()),
                "time", Instant.now().toString()
        ));
    }
}
