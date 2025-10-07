package org.jmouse.security.jwt;

/**
 * 🛡️ ValidatingJwtCodec
 *
 * Decorator around a {@link JwtCodec} that applies additional claim validation
 * (e.g. {@code iss}, {@code aud}) after decoding a JWT.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📥 Delegate {@link #decode(String)} to the underlying codec</li>
 *   <li>✅ Run a {@link JwtClaimsVerifier} if configured</li>
 *   <li>✍️ Pass-through {@link #encode(Jwt)} unchanged</li>
 * </ul>
 */
public final class ValidatingJwtCodec implements JwtCodec {

    private final JwtCodec delegate;
    private final JwtClaimsVerifier verifier;

    /**
     * 🏗️ Create a validating codec.
     *
     * @param delegate underlying codec to perform encode/decode
     * @param verifier optional verifier for claim validation (may be {@code null})
     */
    public ValidatingJwtCodec(JwtCodec delegate, JwtClaimsVerifier verifier) {
        this.delegate = delegate;
        this.verifier = verifier;
    }

    /**
     * 📥 Decode a JWT string, then validate claims.
     *
     * @param token compact JWT string
     * @return parsed {@link Jwt}
     * @throws JwtValidationException if decode or claim validation fails
     */
    @Override
    public Jwt decode(String token) throws JwtValidationException {
        Jwt jwt = delegate.decode(token);

        if (verifier != null) {
            verifier.verify(jwt);
        }

        return jwt;
    }

    /**
     * ✍️ Encode a {@link Jwt} via delegate (no additional validation).
     *
     * @param jwt structured token
     * @return compact JWT string
     * @throws JwtValidationException if encoding fails
     */
    @Override
    public String encode(Jwt jwt) throws JwtValidationException {
        return delegate.encode(jwt);
    }
}
