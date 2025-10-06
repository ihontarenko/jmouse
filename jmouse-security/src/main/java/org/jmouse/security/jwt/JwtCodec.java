package org.jmouse.security.jwt;

import java.util.Map;

public interface JwtCodec {

    Jwt decode(String token) throws JwtDecoder.JwtValidationException;

    String encode(Jwt jwt);

    enum Algorithm {
        HS256, RS256, ES256, EdDSA
    }

    /**
     * 🔌 Minimal JSON abstraction to avoid hard dependency.
     */
    interface Json {

        Map<String, Object> readObject(String json);

        String writeObject(Map<String, Object> object);

    }

    class JwtValidationException extends RuntimeException {

        public JwtValidationException(String message) {
            super(message);
        }

        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
