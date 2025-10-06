package org.jmouse.security.jwt;

import java.time.Instant;
import java.util.Map;

/**
 * 📦 Immutable representation of a JSON Web Token (JWT).
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>🔑 The raw token value</li>
 *   <li>📜 Header parameters</li>
 *   <li>🧾 Claims (payload)</li>
 *   <li>⏱️ Standard timing fields: {@link #issuedAt}, {@link #expiresAt}</li>
 *   <li>🙍 Subject identifier (sub claim)</li>
 * </ul>
 */
public record Jwt(
        /** 🔑 The compact JWT string. */
        String tokenValue,

        /** 📜 Header parameters (e.g., alg, typ, kid). */
        Map<String, Object> headers,

        /** 🧾 Claims (JWT body/payload). */
        Map<String, Object> claims,

        /** ⏱️ Token issuance timestamp (iat). */
        Instant issuedAt,

        /** ⏱️ Token expiration timestamp (exp). */
        Instant expiresAt,

        /** 🙍 Subject claim (sub). */
        String subject) {

    /**
     * 🔍 Retrieve a claim by name with generic typing.
     *
     * <p>⚠️ Caller is responsible for type-safety of the cast.</p>
     *
     * @param name claim name
     * @param <T>  expected claim type
     * @return claim value or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T claim(String name) {
        return (T) claims.get(name);
    }
}
