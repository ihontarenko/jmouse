package org.jmouse.web.http.request;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.Streamable;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

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
     * 🏷️ Optional HTTP status associated with this header set.
     * <p>
     * Typically used for buffered responses before committing to the client.
     * </p>
     */
    private HttpStatus status = null;

    /**
     * 🧭 Optional HTTP method associated with this header set.
     * <p>
     * Useful when representing request metadata alongside headers.
     * </p>
     */
    private HttpMethod method = null;

    /**
     * 📡 Returns the buffered HTTP status code.
     *
     * <p>May be {@code null} if no status has been set yet.</p>
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 📝 Sets the HTTP status code to buffer alongside headers.
     *
     * <p>This does not immediately commit the status to the client;
     * it is applied when headers are written to the response.</p>
     */
    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    /**
     * 🧭 Returns the HTTP method associated with this header set.
     *
     * <p>May be {@code null} if not explicitly assigned.</p>
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * 📝 Sets the HTTP method to buffer alongside headers.
     *
     * <p>Primarily useful in request handling scenarios where
     * the method is tracked together with headers.</p>
     */
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    /**
     * ➕ Add or replace a header in this collection.
     *
     * <p>If a header with the same name already exists, its value
     * will be overwritten with the provided {@code value}.</p>
     *
     * @param header the {@link HttpHeader} key (never {@code null})
     * @param value  the header value (may be a {@link String}, {@link Number},
     *               {@link java.util.List}, etc.)
     */
    public void setHeader(HttpHeader header, Object value) {
        headers.put(header, value);
    }

    /**
     * 📥 Retrieve the value of a header.
     *
     * @param header the {@link HttpHeader} to look up
     * @return the associated value, or {@code null} if not present
     */
    public Object getHeader(HttpHeader header) {
        return headers.get(header);
    }

    /**
     * ❌ Remove a header from this collection.
     *
     * @param header the {@link HttpHeader} to remove
     * @return the previous value associated with the header,
     *         or {@code null} if none was present
     */
    public Object removeHeader(HttpHeader header) {
        return headers.remove(header);
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
     * 🧹 Clears all headers, status and http-method.
     */
    public void clear() {
        status = null;
        method = null;
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

        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {}
        }

        return -1L;
    }

    /**
     * 📏 Set the {@code Content-Length} header.
     *
     * @param length length of the response body in bytes
     */
    public void setContentLength(long length) {
        setHeader(HttpHeader.CONTENT_LENGTH, length);
    }

    /**
     * 📐 Set the {@code Range} header using a list of {@link Range} objects.
     *
     * @param range list of byte ranges
     */
    public void setRange(List<Range> range) {
        setHeader(HttpHeader.RANGE, range);
    }

    /**
     * 📐 Set the {@code Range} header using a raw header string.
     *
     * <p>Example: {@code "bytes=0-499,1000-1499"}</p>
     *
     * @param range raw header string
     */
    public void setRange(String range) {
        setRange(Range.parseRanges(range));
    }

    /**
     * 📥 Get the parsed {@code Range} header.
     *
     * <p>If the header is stored as a {@link String}, it will be parsed
     * and cached as a {@code List<Range>}.</p>
     *
     * @return list of {@link Range} objects, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<Range> getRange() {
        Object      value  = headers.get(HttpHeader.RANGE);
        List<Range> ranges = List.of();

        if (value instanceof List) {
            ranges = (List<Range>) value;
        } else if (value instanceof String string) {
            ranges = Range.parseRanges(string);
            setRange(ranges);
        }

        return ranges;
    }


    /**
     * 📎 Set the {@code Content-Disposition} header as raw string.
     *
     * <p>Example: {@code "inline"}, {@code "attachment; filename=\"file.txt\""}.</p>
     *
     * @param disposition disposition header value
     */
    public void setContentDisposition(String disposition) {
        setHeader(HttpHeader.CONTENT_DISPOSITION, disposition);
    }

    /**
     * 📎 Set the {@code Content-Disposition} header from a typed object.
     *
     * @param disposition typed {@link ContentDisposition}
     */
    public void setContentDisposition(ContentDisposition disposition) {
        setContentDisposition(disposition.toString());
    }

    /**
     * 📎 Get the {@code Content-Disposition} header as a typed object.
     *
     * <p>If stored as a {@link String}, it will be parsed and replaced with a
     * cached {@link ContentDisposition} instance.</p>
     *
     * @return parsed {@link ContentDisposition} or {@code null} if not present
     */
    public ContentDisposition getContentDisposition() {
        Object value = headers.get(HttpHeader.CONTENT_DISPOSITION);
        ContentDisposition disposition = null;

        if (value instanceof ContentDisposition) {
            disposition = (ContentDisposition) value;
        } else if (value instanceof String string) {
            disposition = ContentDisposition.parse(string);
            setContentDisposition(disposition);
        }

        return disposition;
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
     * 🏷️ Get the current {@code ETag} header value.
     *
     * @return entity tag string or {@code null} if not set
     */
    public String getETag() {
        return (String) getHeader(HttpHeader.ETAG);
    }

    /**
     * 🏷️ Set the {@code ETag} header value.
     *
     * <ul>
     *   <li>Must be enclosed in quotes, e.g. {@code "abc123"}</li>
     *   <li>Or prefixed with {@code W/} for weak validators, e.g. {@code W/"xyz"}</li>
     * </ul>
     *
     * @param etag entity tag value
     */
    public void setETag(String etag) {
        if ((etag.startsWith("\"") || etag.startsWith("W/")) && etag.endsWith("\"")) {
            setHeader(HttpHeader.ETAG, etag);
        }
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
