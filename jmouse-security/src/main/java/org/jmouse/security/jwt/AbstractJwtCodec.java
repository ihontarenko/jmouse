package org.jmouse.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 🧩 Shared JWT logic: Base64URL, header/claims parse, time checks, signing input.
 */
public abstract class AbstractJwtCodec implements JwtCodec {

    public static final String JWT_TYPE = "JWT";

    protected final Algorithm   algorithm;
    protected final Clock       clock;
    protected final long        allowedSkewSeconds;
    protected final AdapterJson adapter;

    protected AbstractJwtCodec(AdapterJson adapter, Algorithm algorithm) {
        this(adapter, algorithm, Clock.systemUTC(), 60); // default skew 60s
    }

    protected AbstractJwtCodec(AdapterJson adapter, Algorithm algorithm, Clock clock, long skewSeconds) {
        this.algorithm = algorithm;
        this.clock = clock;
        this.allowedSkewSeconds = skewSeconds;
        this.adapter = adapter;
    }

    @Override
    public final Jwt decode(String token) throws JwtValidationException {
        try {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new JwtValidationException("Malformed JWT");
            }

            byte[] headerBytes  = b64Decode(parts[0]);
            byte[] payloadBytes = b64Decode(parts[1]);
            byte[] signature    = b64Decode(parts[2]);

            String signingInput = parts[0] + "." + parts[1];

            Map<String, Object> header = adapter.readObject(new String(headerBytes, UTF_8));
            Map<String, Object> claims = adapter.readObject(new String(payloadBytes, UTF_8));

            // alg check
            String algorithm = String.valueOf(header.getOrDefault("alg", ""));
            if (!this.algorithm.name().equals(algorithm)) {
                throw new JwtValidationException("Unexpected algorithm: " + algorithm + " (expected " + this.algorithm + ")");
            }
            // typ check (optional but nice)
            Object typ = header.get("typ");
            if (typ != null && !JWT_TYPE.equalsIgnoreCase(String.valueOf(typ))) {
                throw new JwtValidationException("Unexpected type: " + typ);
            }

            // signature verify
            if (!verify(signingInput.getBytes(UTF_8), signature)) {
                throw new JwtValidationException("Invalid signature");
            }

            // timing claims
            Instant now = clock.instant();
            Instant exp = readNumericDate(claims.get("exp"));
            Instant nbf = readNumericDate(claims.get("nbf"));
            Instant iat = readNumericDate(claims.get("iat"));

            if (exp != null && now.isAfter(exp.plusSeconds(allowedSkewSeconds))) {
                throw new JwtValidationException("Token expired");
            }

            if (nbf != null && now.plusSeconds(allowedSkewSeconds).isBefore(nbf)) {
                throw new JwtValidationException("Token not yet valid");
            }

            String subject = claims.get("sub") instanceof String string ? string : null;

            return new Jwt(token, header, claims, iat, exp, subject);
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtValidationException("JWT decode failed", e);
        }
    }

    @Override
    public final String encode(Jwt jwt) throws JwtValidationException {
        try {
            Map<String, Object> header = new LinkedHashMap<>(jwt.headers());

            header.put("alg", algorithm.name());
            header.put("typ", "JWT");

            Map<String, Object> claims = new LinkedHashMap<>(jwt.claims());

            if (jwt.issuedAt() != null) {
                claims.put("iat", jwt.issuedAt().getEpochSecond());
            }

            if (jwt.expiresAt() != null) {
                claims.put("exp", jwt.expiresAt().getEpochSecond());
            }

            if (jwt.subject() != null) {
                claims.putIfAbsent("sub", jwt.subject());
            }

            String headerJson  = adapter.writeObject(header);
            String claimsJson  = adapter.writeObject(claims);

            String headerValue  = b64Encode(headerJson.getBytes(UTF_8));
            String payloadData  = b64Encode(claimsJson.getBytes(UTF_8));
            String signingInput = headerValue + "." + payloadData;

            byte[] signature = sign(signingInput.getBytes(UTF_8));

            String base64 = b64Encode(signature);

            return signingInput + "." + base64;
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtValidationException("JWT encode failed", e);
        }
    }

    protected abstract byte[] sign(byte[] signingInput) throws Exception;

    protected abstract boolean verify(byte[] signingInput, byte[] signature) throws Exception;

    protected static Instant readNumericDate(Object dateValue) {
        if (dateValue != null) {
            if (dateValue instanceof Number n)
                return Instant.ofEpochSecond(n.longValue());
            if (dateValue instanceof String s && !s.isBlank()) {
                try {
                    return Instant.ofEpochSecond(Long.parseLong(s));
                } catch (NumberFormatException ignore) {
                }
            }
        }

        return null;
    }

    protected static String b64Encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    protected static byte[] b64Decode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    protected static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }

    protected static void require(boolean condition, String message) throws JwtValidationException {
        if (!condition) {
            throw new JwtValidationException(message);
        }
    }

}
