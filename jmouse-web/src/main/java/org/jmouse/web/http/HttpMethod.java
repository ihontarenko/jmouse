package org.jmouse.web.http;

public enum HttpMethod {

    GET(true, true, false),
    POST(false, false, true),
    PUT(false, true, true),
    DELETE(false, true, false),
    PATCH(false, false, true),
    HEAD(true, true, false),
    OPTIONS(true, true, false),
    TRACE(true, true, false);

    private final boolean isSafe;
    private final boolean isIdempotent;
    private final boolean hasBody;

    HttpMethod(boolean isSafe, boolean isIdempotent, boolean hasBody) {
        this.isSafe = isSafe;
        this.isIdempotent = isIdempotent;
        this.hasBody = hasBody;
    }

    public static HttpMethod ofName(String name) {
        return valueOf(name);
    }

    /**
     * Checks whether this enum constant matches the given method name, ignoring case.
     *
     * <p>Null-safe: returns {@code false} when {@code name} is {@code null}.</p>
     *
     * @param name method name to compare (e.g., {@code "GET"}, {@code "post"})
     * @return {@code true} if names are equal case-insensitively; {@code false} otherwise
     */
    public boolean matches(String name) {
        return name().equalsIgnoreCase(name);
    }

    /**
     * Checks referential equality with another {@link HttpMethod}.
     *
     * @param httpMethod method to compare
     * @return {@code true} if the same enum constant; {@code false} otherwise
     */
    public boolean matches(HttpMethod httpMethod) {
        return this == httpMethod;
    }

    /**
     * Checks if the HTTP method is safe (does not modify the server state).
     * @return true if the method is safe, false otherwise.
     */
    public boolean isSafe() {
        return isSafe;
    }

    /**
     * Checks if the HTTP method is idempotent (multiple requests have the same effect as one).
     * @return true if the method is idempotent, false otherwise.
     */
    public boolean isIdempotent() {
        return isIdempotent;
    }

    /**
     * Checks if the HTTP method allows a request body.
     * @return true if the method allows a body, false otherwise.
     */
    public boolean hasBody() {
        return hasBody;
    }

}
