package com.tourguide.backend.security;

import com.tourguide.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Issues and verifies HS256 JWTs. Access tokens carry the user id (subject), {@link UserType},
 * and roles; refresh tokens are marked with a {@code use=refresh} claim and are only valid at the
 * refresh endpoint. Secret + TTLs come from {@code app.jwt.*} (see {@link AppProperties}).
 */
@Service
public class JwtService {

    static final String CLAIM_TYPE = "type";
    static final String CLAIM_ROLES = "roles";
    static final String CLAIM_TOKEN_USE = "use";
    static final String REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = props.jwt().accessTokenTtl() != null ? props.jwt().accessTokenTtl() : Duration.ofHours(2);
        this.refreshTtl = props.jwt().refreshTokenTtl() != null ? props.jwt().refreshTokenTtl() : Duration.ofDays(7);
    }

    public String issueAccessToken(long userId, UserType type, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public String issueRefreshToken(long userId, UserType type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_TOKEN_USE, REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(key)
                .compact();
    }

    /** Verify signature + expiry and return the claims. Throws {@link JwtException} if invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH.equals(claims.get(CLAIM_TOKEN_USE, String.class));
    }

    /** Verify + decode a token into its typed contents. Throws if signature/expiry are invalid. */
    public TokenInfo introspect(String token) {
        Claims c = parse(token);
        @SuppressWarnings("unchecked")
        List<String> roles = c.get(CLAIM_ROLES, List.class);
        return new TokenInfo(
                Long.parseLong(c.getSubject()),
                UserType.valueOf(c.get(CLAIM_TYPE, String.class)),
                roles == null ? List.of() : roles,
                isRefreshToken(c));
    }

    /** Decoded token contents. */
    public record TokenInfo(long userId, UserType type, List<String> roles, boolean refresh) {
    }
}
