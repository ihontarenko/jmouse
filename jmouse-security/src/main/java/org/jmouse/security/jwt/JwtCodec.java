package org.jmouse.security.jwt;

import java.util.Map;

public interface JwtCodec {

    Jwt decode(String token) throws JwtValidationException;

    String encode(Jwt jwt) throws JwtValidationException;

    enum Algorithm {
        HS256, RS256, ES256, EdDSA
    }

    /**
     * 🔌 Minimal JSON abstraction to avoid hard dependency.
     */
    interface AdapterJson {

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
