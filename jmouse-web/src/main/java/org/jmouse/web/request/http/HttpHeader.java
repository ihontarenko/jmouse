package org.jmouse.web.request.http;

/**
 * Common HTTP header names 💡
 *
 * <p>Example:
 * <pre>{@code
 * response.setHeader(HttpHeaders.CONTENT_TYPE.value(), "application/json");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public enum HttpHeader {

    // --- General headers ---
    CACHE_CONTROL("Cache-Control"),
    CONNECTION("Connection"),
    DATE("Date"),
    PRAGMA("Pragma"),
    TRAILER("Trailer"),
    TRANSFER_ENCODING("Transfer-Encoding"),
    UPGRADE("Upgrade"),
    VIA("Via"),
    WARNING("Warning"),

    // --- Request headers ---
    ACCEPT("Accept"),
    ACCEPT_CHARSET("Accept-Charset"),
    ACCEPT_ENCODING("Accept-Encoding"),
    ACCEPT_LANGUAGE("Accept-Language"),
    AUTHORIZATION("Authorization"),
    EXPECT("Expect"),
    FROM("From"),
    HOST("Host"),
    IF_MATCH("If-Match"),
    IF_MODIFIED_SINCE("If-Modified-Since"),
    IF_NONE_MATCH("If-None-Match"),
    IF_RANGE("If-Range"),
    IF_UNMODIFIED_SINCE("If-Unmodified-Since"),
    MAX_FORWARDS("Max-Forwards"),
    PROXY_AUTHORIZATION("Proxy-Authorization"),
    RANGE("Range"),
    REFERER("Referer"),
    TE("TE"),
    USER_AGENT("User-Agent"),

    // --- Response headers ---
    ACCEPT_RANGES("Accept-Ranges"),
    AGE("Age"),
    ETAG("ETag"),
    LOCATION("Location"),
    PROXY_AUTHENTICATE("Proxy-Authenticate"),
    RETRY_AFTER("Retry-After"),
    SERVER("Server"),
    VARY("Vary"),
    WWW_AUTHENTICATE("WWW-Authenticate"),

    // --- Entity headers ---
    ALLOW("Allow"),
    CONTENT_ENCODING("Content-Encoding"),
    CONTENT_LANGUAGE("Content-Language"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_LOCATION("Content-Location"),
    CONTENT_MD5("Content-MD5"),
    CONTENT_RANGE("Content-Range"),
    CONTENT_TYPE("Content-Type"),
    EXPIRES("Expires"),
    LAST_MODIFIED("Last-Modified"),

    // --- Custom / common headers ---
    ORIGIN("Origin"),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),
    ACCESS_CONTROL_ALLOW_HEADERS("Access-Control-Allow-Headers"),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods"),
    ACCESS_CONTROL_EXPOSE_HEADERS("Access-Control-Expose-Headers"),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age"),
    ACCESS_CONTROL_REQUEST_HEADERS("Access-Control-Request-Headers"),
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method"),
    X_FORWARDED_FOR("X-Forwarded-For"),
    X_FORWARDED_PROTO("X-Forwarded-Proto"),
    X_REQUESTED_WITH("X-Requested-With"),
    X_FRAME_OPTIONS("X-Frame-Options"),
    SET_COOKIE("Set-Cookie"),
    COOKIE("Cookie");

    private final String value;

    HttpHeader(String value) {
        this.value = value;
    }

    /**
     * Returns the actual HTTP header name 📦
     */
    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return value;
    }

}

