package org.jmouse.web.http.request;

import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Conditional request evaluator for HTTP validators (ETag and Last-Modified).
 *
 * <p><strong>Decision order</strong>:
 * <ol>
 *   <li>If-Match / If-Unmodified-Since (preconditions for unsafe methods).</li>
 *   <li>If-None-Match (has priority over If-Modified-Since).</li>
 *   <li>If-Modified-Since (applies only when If-None-Match is absent and method is safe: GET/HEAD).</li>
 * </ol>
 *
 * <p><strong>Semantics</strong>:
 * <ul>
 *   <li>For GET/HEAD: a satisfied {@code If-None-Match} yields {@code 304 Not Modified}; otherwise {@code 200 OK}.</li>
 *   <li>For unsafe methods: a failing {@code If-Match} or {@code If-Unmodified-Since} yields {@code 412 Precondition Failed}.</li>
 *   <li>{@code If-Modified-Since} has second precision (RFC 7232). {@code 304} only if resource not modified <em>since</em> that time.</li>
 *   <li>On {@code 304} or {@code 200}, the response will include current {@code ETag} and {@code Last-Modified} when available.</li>
 * </ul>
 *
 * <p>This class is stateless and thread-safe.</p>
 *
 * @see ETag
 * @see IfMatch
 * @see IfNoneMatch
 * @see Headers
 * @see HttpHeader
 * @see HttpMethod
 * @see HttpStatus
 * @see PreconditionResult
 */
public final class ConditionalRequest {

    /**
     * HTTP-date formatter in GMT per RFC 7231 §7.1.1.1 (a.k.a. RFC 1123).
     */
    private static final DateTimeFormatter HTTP_DATE = RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    /**
     * Safe methods per RFC semantics; conditional GET/HEAD produce 304 when applicable.
     */
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

    private ConditionalRequest() {
    }

    /**
     * Evaluates request preconditions against the current validators and sets the appropriate response status/headers.
     *
     * <p>Side effects:</p>
     * <ul>
     *   <li>Writes {@code ETag} and {@code Last-Modified} (if present) to {@code responseHeaders} up-front.</li>
     *   <li>May set {@code 304 Not Modified} (safe methods) or {@code 412 Precondition Failed} (unsafe) and return immediately.</li>
     * </ul>
     *
     * @param requestHeaders     incoming request headers (used to read conditional headers and method)
     * @param responseHeaders    outgoing response headers (status and validators will be written here)
     * @param lastModifiedMillis resource last-modified time in milliseconds since epoch; {@code <=0} means unknown
     * @param etag               current strong/weak entity tag; may be {@code null} if unknown
     * @return {@link PreconditionResult} indicating {@code NOT_MODIFIED_304}, {@code PRECONDITION_FAILED_412}, or {@code PROCEED_200}.
     * @see HttpHeader#IF_MATCH
     * @see HttpHeader#IF_UNMODIFIED_SINCE
     * @see HttpHeader#IF_NONE_MATCH
     * @see HttpHeader#IF_MODIFIED_SINCE
     */
    public static PreconditionResult evaluate(Headers requestHeaders, Headers responseHeaders, long lastModifiedMillis, ETag etag) {

        long lastModifiedSeconds = toHttpSecond(lastModifiedMillis);

        // Publish current validators so clients can cache even on 200/304.
        if (etag != null) {
            responseHeaders.setHeader(HttpHeader.ETAG, etag.toHeaderValue());
        }

        if (lastModifiedSeconds > 0) {
            responseHeaders.setHeader(HttpHeader.LAST_MODIFIED, toHttpDate(lastModifiedSeconds));
        }

        // --- If-Match / If-Unmodified-Since (preconditions for unsafe methods)
        final String ifMatchRaw = header(requestHeaders, HttpHeader.IF_MATCH);
        if (ifMatchRaw != null) {
            final IfMatch ifMatch     = IfMatch.parse(ifMatchRaw);
            final boolean isSatisfied = ifMatch.isEmpty() || ifMatch.isSatisfied(etag, false, true);
            if (!isSatisfied) {
                setPreconditionFailed412(responseHeaders);
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
        }

        final String ifUnmodifiedSinceRaw = header(requestHeaders, HttpHeader.IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSinceRaw != null && lastModifiedSeconds > 0) {
            long ifUnmodifiedSinceSeconds = toHttpSeconds(ifUnmodifiedSinceRaw);
            if (ifUnmodifiedSinceSeconds >= 0 && lastModifiedSeconds > ifUnmodifiedSinceSeconds) {
                setPreconditionFailed412(responseHeaders);
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
        }

        // --- If-None-Match has priority over If-Modified-Since
        final String ifNoneMatchRaw = header(requestHeaders, HttpHeader.IF_NONE_MATCH);
        if (ifNoneMatchRaw != null) {
            final boolean     allowWeak   = isSafeMethod(requestHeaders.getMethod());
            final IfNoneMatch ifNoneMatch = IfMatch.parse(ifNoneMatchRaw).toNoneMatch();
            final boolean     isSatisfied = ifNoneMatch.isSatisfied(etag, allowWeak, true);

            if (!isSatisfied) {
                if (allowWeak) {
                    setNotModified304(responseHeaders);
                    return PreconditionResult.NOT_MODIFIED_304;
                }
                setPreconditionFailed412(responseHeaders);
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
            // If-None-Match did not match -> proceed (ignore If-Modified-Since)
            return PreconditionResult.PROCEED_200;
        }

        // --- If-Modified-Since (only when If-None-Match is absent)
        final String ifModifiedSinceRaw = header(requestHeaders, HttpHeader.IF_MODIFIED_SINCE);
        if (ifModifiedSinceRaw != null && lastModifiedSeconds > 0 && isSafeMethod(requestHeaders.getMethod())) {
            long ifModifiedSinceSeconds = toHttpSeconds(ifModifiedSinceRaw);
            if (ifModifiedSinceSeconds >= 0 && lastModifiedSeconds <= ifModifiedSinceSeconds) {
                setNotModified304(responseHeaders);
                return PreconditionResult.NOT_MODIFIED_304;
            }
        }

        return PreconditionResult.PROCEED_200;
    }

    /**
     * Sets {@code 304 Not Modified}.
     *
     * @param responseHeaders response headers to mutate
     */
    private static void setNotModified304(Headers responseHeaders) {
        responseHeaders.setStatus(HttpStatus.NOT_MODIFIED);
    }

    /**
     * Sets {@code 412 Precondition Failed}.
     *
     * @param responseHeaders response headers to mutate
     */
    private static void setPreconditionFailed412(Headers responseHeaders) {
        responseHeaders.setStatus(HttpStatus.PRECONDITION_FAILED);
    }

    /**
     * Returns the string value of a header or {@code null} if absent.
     * Never returns literal {@code "null"}.
     */
    private static String header(Headers requestHeaders, HttpHeader header) {
        Object v = requestHeaders.getHeader(header);
        return v != null ? v.toString() : null;
    }

    /**
     * Converts milliseconds to whole HTTP seconds (floor). Returns {@code 0} when input is non-positive.
     */
    private static long toHttpSecond(long millis) {
        return millis > 0 ? (millis / 1000L) : 0;
    }

    /**
     * Formats seconds since epoch to RFC 1123 date in GMT.
     */
    private static String toHttpDate(long epochSeconds) {
        return HTTP_DATE.format(Instant.ofEpochSecond(epochSeconds));
    }

    /**
     * Parses an RFC 1123 HTTP-date to epoch seconds. Returns {@code -1} on parse error.
     */
    private static long toHttpSeconds(String raw) {
        try {
            return Instant.from(HTTP_DATE.parse(raw.trim())).getEpochSecond();
        } catch (Exception ignore) {
            return -1;
        }
    }

    /**
     * @return {@code true} for GET/HEAD; {@code false} otherwise.
     */
    private static boolean isSafeMethod(HttpMethod method) {
        return SAFE_METHODS.contains(method);
    }
}
