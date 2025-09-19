package org.jmouse.web.http;

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
    PROXY_AUTHORIZATION("ProxyProvider-Authorization"),
    RANGE("Range"),
    REFERER("Referer"),
    TE("TE"),
    USER_AGENT("User-Agent"),

    // --- Response headers ---
    ACCEPT_RANGES("Accept-Ranges"),
    AGE("Age"),
    ETAG("ETag"),
    LOCATION("Location"),
    PROXY_AUTHENTICATE("ProxyProvider-Authenticate"),
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
    CONTENT_DISPOSITION("Content-Disposition"),
    CONTENT_MD5("Content-MD5"),
    CONTENT_RANGE("Content-Range"),
    CONTENT_TYPE("Content-Type"),
    EXPIRES("Expires"),
    LAST_MODIFIED("Last-Modified"),

    // --- Security / Firewalls
    X_FIREWALL_REASON("X-Firewall-Reason"),

    // --- Custom / common headers ---
    ORIGIN("Origin"),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),
    ACCESS_CONTROL_ALLOW_HEADERS("Access-Control-Allow-Headers"),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods"),
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),
    ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK("Access-Control-Allow-Private-Network"),
    ACCESS_CONTROL_EXPOSE_HEADERS("Access-Control-Expose-Headers"),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age"),
    ACCESS_CONTROL_REQUEST_HEADERS("Access-Control-Request-Headers"),
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method"),
    ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK("Access-Control-Request-Private-Network"),
    X_REAL_IP("X-Real-IP"),
    X_FORWARDED_FOR("X-Forwarded-For"),
    X_FORWARDED_PROTO("X-Forwarded-Proto"),
    X_REQUESTED_WITH("X-Requested-With"),
    X_FRAME_OPTIONS("X-Frame-Options"),
    SET_COOKIE("Set-Cookie"),
    COOKIE("Cookie"),

    X_HTTP_METHOD_OVERRIDE("X-HTTP-Method-Override"),
    X_TEXT("X-Text"),
    X_JMOUSE_DEBUG("X-jMouse-Debug"),

    // --- Client Hints (UA-CH) ---
    SEC_CH_UA("Sec-Ch-Ua"),
    SEC_CH_UA_MOBILE("Sec-Ch-Ua-Mobile"),
    SEC_CH_UA_PLATFORM("Sec-Ch-Ua-Platform"),
    SEC_CH_UA_PLATFORM_VERSION("Sec-Ch-Ua-Platform-Version"),
    SEC_CH_UA_FULL_VERSION("Sec-Ch-Ua-Full-Version"),
    SEC_CH_UA_FULL_VERSION_LIST("Sec-Ch-Ua-Full-Version-List"),
    SEC_CH_UA_ARCH("Sec-Ch-Ua-Arch"),
    SEC_CH_UA_MODEL("Sec-Ch-Ua-Model"),
    SEC_CH_UA_BITNESS("Sec-Ch-Ua-Bitness"),
    SEC_CH_UA_WOW64("Sec-Ch-Ua-Wow64"),

    // --- Fetch Metadata ---
    SEC_FETCH_DEST("Sec-Fetch-Dest"),
    SEC_FETCH_MODE("Sec-Fetch-Mode"),
    SEC_FETCH_SITE("Sec-Fetch-Site"),
    SEC_FETCH_USER("Sec-Fetch-User");

    private final String value;

    HttpHeader(String value) {
        this.value = value;
    }

    public static HttpHeader ofHeader(String headerName) {
        HttpHeader httpHeader = null;

        for (HttpHeader header : values()) {
            if (headerName.equalsIgnoreCase(header.value())) {
                httpHeader = header;
                break;
            }
        }

        return httpHeader;
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

