package com.bank4z.backend.common.security;

import com.bank4z.backend.authservice.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /** Access tokens carry the session id ("sid") so the server can check the session is still active. */
    public String generateAccessToken(User user, UUID sessionId) {
        return buildToken(user, sessionId, null, accessTokenExpiryMs, "access");
    }

    /**
     * Refresh tokens also carry a unique token id ("jti"). The session remembers
     * the one jti that is currently valid, which is what makes rotation and
     * reuse detection possible.
     */
    public String generateRefreshToken(User user, UUID sessionId, UUID tokenId) {
        return buildToken(user, sessionId, tokenId, refreshTokenExpiryMs, "refresh");
    }

    private String buildToken(User user, UUID sessionId, UUID tokenId, long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("sid", sessionId.toString())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry);

        if (tokenId != null) {
            builder.id(tokenId.toString());
        }

        return builder.signWith(signingKey).compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Null if the token has no session id (e.g. issued before sessions existed). */
    public UUID extractSessionId(String token) {
        String sid = extractClaim(token, claims -> claims.get("sid", String.class));
        return sid == null ? null : UUID.fromString(sid);
    }

    /** Null for access tokens, which have no jti. */
    public UUID extractTokenId(String token) {
        String jti = extractClaim(token, Claims::getId);
        return jti == null ? null : UUID.fromString(jti);
    }

    /**
     * Validates signature, expiry, AND that the token is the type expected
     * for this use (an access token can't be smuggled in as a refresh token
     * or vice versa).
     */
    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean correctType = expectedType.equals(claims.get("type", String.class));
            return notExpired && correctType;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}