package com.tourguide.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Typed, validated binding for the {@code app.*} configuration tree.
 *
 * <p>Secrets are injected from environment variables (see {@code .env.example}).
 * Required values (e.g. the JWT secret) are validated at startup, so a misconfigured
 * prod deployment fails fast instead of running with an empty key.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid Jwt jwt, @Valid Wechat wechat) {

    public record Jwt(
            @NotBlank(message = "app.jwt.secret (env JWT_SECRET) must be set") String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {
    }

    public record Wechat(Miniapp miniapp, Pay pay) {

        public record Miniapp(String appId, String appSecret) {
        }

        public record Pay(String mchId, String apiV3Key, String certSerialNo, String notifyUrl) {
        }
    }
}
