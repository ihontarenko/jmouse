package org.jmouse.security.jwt;

import java.util.*;

/**
 * ✅ JwtClaimsVerifier
 * <p>
 * Validates issuer ({@code iss}) and audience ({@code aud}) claims using either a
 * {@link Mode#STRICT STRICT} or {@link Mode#LOOSE LOOSE} policy.
 *
 * <p><b>Policies:</b></p>
 * <ul>
 *   <li>🔒 <b>STRICT</b>
 *     <ul>
 *       <li>iss: <i>must</i> be present and equal to one of expected issuers (exact match,
 *           ignoring a single trailing '/')</li>
 *       <li>aud: <i>must</i> be present and intersect with expected audiences (non-empty)</li>
 *     </ul>
 *   </li>
 *   <li>🌿 <b>LOOSE</b>
 *     <ul>
 *       <li>iss: may be absent; if present, must match as in STRICT</li>
 *       <li>aud: may be absent; if present, must intersect as in STRICT</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class JwtClaimsVerifier {

    private final Mode        mode;
    private final Set<String> issuersNorm;
    private final Set<String> audiences;

    private JwtClaimsVerifier(Mode mode, Set<String> issuers, Set<String> audiences) {
        this.mode = mode;
        this.issuersNorm = normalizeIssuers(issuers);
        this.audiences = (audiences == null) ? Set.of() : Set.copyOf(audiences);
    }

    /**
     * 🏗️ Create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 🧾 Extract {@code aud} as a set of strings.
     * Accepts either a single string or a collection of strings; otherwise empty.
     */
    private static Set<String> extractAudience(Object value) {
        return switch (value) {
            case String string -> Set.of(string);
            case Collection<?> collection -> {
                Set<String> result = new LinkedHashSet<>(collection.size());
                for (Object object : collection) {
                    if (object instanceof String string) {
                        result.add(string);
                    }
                }
                yield result;
            }
            case null, default -> Set.of();
        };
    }

    /**
     * 🧼 Normalize issuers by trimming a single trailing slash (if present).
     */
    private static Set<String> normalizeIssuers(Set<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>(collection.size());

        for (String issuer : collection) {
            result.add(normalizeIssuer(issuer));
        }

        return Set.copyOf(result);
    }

    /**
     * 🧼 Normalize a single issuer: remove one trailing '/' if present.
     * <p><b>Fix:</b> returns the original value when no slash is present (was returning {@code null}).</p>
     */
    private static String normalizeIssuer(String value) {
        if (value == null) {
            return null;
        }

        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; // ✅ fixed
    }

    /**
     * ✅ Verify {@code iss} and {@code aud} claims against configured expectations.
     *
     * @param jwt parsed JWT (headers/claims already available)
     * @throws JwtCodec.JwtValidationException if policy checks fail
     */
    public void verify(Jwt jwt) throws JwtCodec.JwtValidationException {
        Map<String, Object> claims = jwt.claims();
        String              iss    = (claims.get("iss") instanceof String s) ? s : null;

        if (mode == Mode.STRICT) {
            if (iss == null) throw new JwtCodec.JwtValidationException("Missing 'iss'");
            if (!issuersNorm.contains(normalizeIssuer(iss))) {
                throw new JwtCodec.JwtValidationException("Invalid 'iss'");
            }
        } else { // LOOSE
            if (iss != null && !issuersNorm.isEmpty() && !issuersNorm.contains(normalizeIssuer(iss))) {
                throw new JwtCodec.JwtValidationException("Invalid 'iss'");
            }
        }

        Set<String> tokenAudience   = extractAudience(claims.get("aud"));
        boolean     hasIntersection = !Collections.disjoint(tokenAudience, audiences);

        if (mode == Mode.STRICT) {
            if (!audiences.isEmpty()) {
                if (tokenAudience.isEmpty()) {
                    throw new JwtCodec.JwtValidationException("Missing 'aud'");
                }
                if (!hasIntersection) {
                    throw new JwtCodec.JwtValidationException("Invalid 'aud'");
                }
            }
        } else { // LOOSE
            if (tokenAudience.isEmpty()) {
                return; // aud not required
            }
            if (!audiences.isEmpty() && !hasIntersection) {
                throw new JwtCodec.JwtValidationException("Invalid 'aud'");
            }
        }
    }

    /**
     * 🔧 Verification mode.
     */
    public enum Mode {STRICT, LOOSE}

    /**
     * 🔧 Builder for {@link JwtClaimsVerifier}.
     */
    public static final class Builder {

        private final Set<String> issuer   = new LinkedHashSet<>();
        private final Set<String> audience = new LinkedHashSet<>();
        private       Mode        mode     = Mode.STRICT;

        /**
         * Set verification mode (default {@link Mode#STRICT}).
         */
        public Builder mode(Mode m) {
            this.mode = m;
            return this;
        }

        /**
         * Add one or more expected issuers (exact match after single trailing '/' is trimmed).
         */
        public Builder issuer(String... issuers) {
            Collections.addAll(issuer, issuers);
            return this;
        }

        /**
         * Add one or more expected audiences.
         */
        public Builder audience(String... audiences) {
            Collections.addAll(audience, audiences);
            return this;
        }

        /**
         * Build the immutable verifier.
         */
        public JwtClaimsVerifier build() {
            return new JwtClaimsVerifier(mode, issuer, audience);
        }
    }
}
