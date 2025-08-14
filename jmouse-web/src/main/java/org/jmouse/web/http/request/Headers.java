package org.jmouse.web.http.request;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.Streamable;
import org.jmouse.web.http.HttpHeader;

import java.util.*;

/**
 * 📦 Holds HTTP headers in a mutable but encapsulated way.
 *
 * Used across request/response handling.
 * Prefer using typed getters/setters where possible.
 *
 * 🔒 Not thread-safe.
 *
 * <pre>{@code
 * Headers headers = new Headers();
 * headers.setAuthorization("Bearer ...");
 * headers.setContentType(MediaType.JSON);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Headers {

    private final Map<HttpHeader, Object> headers = new HashMap<>();

    /**
     * ➕ Adds or replaces a header.
     */
    public void setHeader(HttpHeader header, Object value) {
        headers.put(header, value);
    }

    /**
     * 📥 Gets a header value.
     */
    public Object getHeader(HttpHeader header) {
        return headers.get(header);
    }

    /**
     * 🔁 Replaces all headers with the given map.
     */
    public void setAll(Map<HttpHeader, Object> newHeaders) {
        headers.clear();
        headers.putAll(newHeaders);
    }

    /**
     * 📤 Returns unmodifiable view of headers.
     */
    public Map<HttpHeader, Object> asMap() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * ✅ Checks if no headers are present.
     */
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    /**
     * 🧹 Clears all headers.
     */
    public void clear() {
        headers.clear();
    }

    /**
     * 🎯 Typed getter for Content-Type.
     */
    public MediaType getContentType() {
        Object contentType = headers.get(HttpHeader.CONTENT_TYPE);

        if (contentType instanceof String type) {
            setHeader(HttpHeader.CONTENT_TYPE, MediaType.forString(type));
        }

        return (MediaType) headers.get(HttpHeader.CONTENT_TYPE);
    }

    /**
     * 📝 Typed setter for Content-Type.
     */
    public void setContentType(MediaType contentType) {
        setHeader(HttpHeader.CONTENT_TYPE, contentType);
    }

    /**
     * 📏 Gets Content-Length header.
     */
    public long getContentLength() {
        Object value = headers.get(HttpHeader.CONTENT_LENGTH);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }

        return 0L;
    }

    /**
     * 📏 Sets Content-Length header.
     */
    public void setContentLength(long length) {
        setHeader(HttpHeader.CONTENT_LENGTH, length);
    }

    /**
     * 🎯 Typed getter for Accept header.
     */
    public List<MediaType> getAccept() {
        Object accept = headers.get(HttpHeader.ACCEPT);

        if (accept == null) {
            return Collections.emptyList();
        }

        return Streamable.of(MimeParser.parseMimeTypes(accept.toString())).map(MediaType::new).toList();
    }

    /**
     * 📝 Typed setter for Accept header.
     */
    public void setAccept(List<MediaType> accept) {
        setHeader(HttpHeader.ACCEPT, String.join(",", Streamable.of(accept).map(MediaType::toString)));
    }

    /**
     * 🔐 Gets Authorization header.
     */
    public String getAuthorization() {
        return (String) headers.get(HttpHeader.AUTHORIZATION);
    }

    /**
     * 🔐 Sets Authorization header.
     */
    public void setAuthorization(String authHeader) {
        setHeader(HttpHeader.AUTHORIZATION, authHeader);
    }

    /**
     * 🧭 Gets User-Agent header.
     */
    public String getUserAgent() {
        return (String) headers.get(HttpHeader.USER_AGENT);
    }

    /**
     * 🧭 Sets User-Agent header.
     */
    public void setUserAgent(String userAgent) {
        setHeader(HttpHeader.USER_AGENT, userAgent);
    }

    /**
     * 🌐 Gets Host header.
     */
    public String getHost() {
        return (String) headers.get(HttpHeader.HOST);
    }

    /**
     * 🌐 Sets Host header.
     */
    public void setHost(String host) {
        setHeader(HttpHeader.HOST, host);
    }

    /**
     * 🍪 Gets Cookie header.
     */
    public String getCookie() {
        return (String) headers.get(HttpHeader.COOKIE);
    }

    /**
     * 🍪 Sets Cookie header.
     */
    public void setCookie(String cookie) {
        setHeader(HttpHeader.COOKIE, cookie);
    }

    /**
     * 🔙 Gets Referer header.
     */
    public String getReferer() {
        return (String) headers.get(HttpHeader.REFERER);
    }

    /**
     * 🔙 Sets Referer header.
     */
    public void setReferer(String referer) {
        setHeader(HttpHeader.REFERER, referer);
    }

    /**
     * 📌 Equality check based on headers map.
     *
     * @param other other object
     * @return true if headers are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof Headers that))
            return false;

        return Objects.equals(headers, that.headers);
    }

    /**
     * 📌 Computes hash code from internal headers map.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(headers);
    }

    @Override
    public String toString() {
        return "Headers" + headers;
    }
}
