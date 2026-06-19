package com.tourguide.backend.api;

import com.tourguide.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Sample endpoint demonstrating the standard {@link ApiResponse} envelope. */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "service", "tourguide-backend",
                "time", Instant.now().toString()
        ));
    }
}
