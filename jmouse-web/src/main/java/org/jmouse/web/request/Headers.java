package org.jmouse.web.request;

import org.jmouse.core.MediaType;
import org.jmouse.web.request.http.HttpHeader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    public void addHeader(HttpHeader header, Object value) {
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
        return (MediaType) headers.get(HttpHeader.CONTENT_TYPE);
    }

    /**
     * 📝 Typed setter for Content-Type.
     */
    public void setContentType(MediaType contentType) {
        addHeader(HttpHeader.CONTENT_TYPE, contentType);
    }

    /**
     * 🎯 Typed getter for Accept header.
     */
    public MediaType getAccept() {
        return (MediaType) headers.get(HttpHeader.ACCEPT);
    }

    /**
     * 📝 Typed setter for Accept header.
     */
    public void setAccept(MediaType accept) {
        addHeader(HttpHeader.ACCEPT, accept);
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
        addHeader(HttpHeader.AUTHORIZATION, authHeader);
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
        addHeader(HttpHeader.USER_AGENT, userAgent);
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
        addHeader(HttpHeader.HOST, host);
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
        addHeader(HttpHeader.COOKIE, cookie);
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
        addHeader(HttpHeader.REFERER, referer);
    }

}
