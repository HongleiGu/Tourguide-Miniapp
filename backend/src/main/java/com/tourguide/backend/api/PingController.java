package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.PingResponse;
import com.tourguide.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Sample endpoint demonstrating the standard {@link ApiResponse} envelope. */
@Tag(name = "Ping", description = "Health/sample endpoint")
@RestController
@RequestMapping("/api/ping")
public class PingController {

    private final Environment env;

    public PingController(Environment env) {
        this.env = env;
    }

    @Operation(summary = "Liveness sample returning the standard response envelope")
    @GetMapping
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.ok(new PingResponse(
                "tourguide-backend",
                List.of(env.getActiveProfiles()),
                Instant.now().toString()));
    }
}
