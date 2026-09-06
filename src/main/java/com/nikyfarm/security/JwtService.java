package com.nikyfarm.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nikyfarm.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies the JWTs used to authenticate {@code /api/**} requests. The token's
 * subject is the user id and it carries a {@code role} claim; {@link JwtAuthenticationFilter}
 * reads both to populate Spring Security's context — see that class for why the principal
 * is the raw user id rather than a {@code UserDetails} object.
 */
@Service
public class JwtService {

    private final Algorithm algorithm;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withClaim("role", user.getRole().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMs)))
                .sign(algorithm);
    }

    public long expirationMs() {
        return expirationMs;
    }

    /** @return the decoded token, or null if missing/expired/tampered. */
    public DecodedJWT verify(String token) {
        try {
            return JWT.require(algorithm).build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public Long extractUserId(DecodedJWT decoded) {
        return Long.parseLong(decoded.getSubject());
    }

    public String extractRole(DecodedJWT decoded) {
        return decoded.getClaim("role").asString();
    }
}
