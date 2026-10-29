package org.jmouse.security.web.csrf;

import java.util.Objects;

/** Token metadata exposed to the application's form or same-origin client. */
public record CsrfToken(String headerName, String parameterName, String value) {
    public static final String REQUEST_ATTRIBUTE = "_csrf";

    public CsrfToken {
        Objects.requireNonNull(headerName, "headerName");
        Objects.requireNonNull(parameterName, "parameterName");
        Objects.requireNonNull(value, "value");
        if (headerName.isBlank() || parameterName.isBlank() || value.isBlank()) {
            throw new IllegalArgumentException("CSRF token fields must not be blank");
        }
    }
}
