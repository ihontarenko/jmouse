package org.jmouse.web.http.request;

import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * 🧠 Conditional request evaluator (ETag + Last-Modified).
 * <p>
 * Implements core RFC rules similarly to Spring:
 * - Priority: If-None-Match first; if absent → If-Modified-Since.
 * - GET/HEAD: If-None-Match match → 304; others → 412.
 * - Non-GET/HEAD: If-None-Match match → 412 (to prevent unsafe updates).
 * - IMS uses seconds granularity; responds 304 only if resource not modified since that time.
 * - Always sets ETag/Last-Modified on 304 or 200 (best effort).
 */
public final class ConditionalRequest {

    // RFC 9110 recommends IMF-fixdate; Java has RFC_1123_DATE_TIME; matches IMF-fixdate.
    private static final DateTimeFormatter HTTP_DATE = RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    private ConditionalRequest() {
    }

    /**
     * Evaluate request preconditions using ETag and Last-Modified.
     *
     * @param requestHeaders     HTTP request
     * @param responseHeaders    HTTP response (headers may be written)
     * @param lastModifiedMillis resource last-modified in milliseconds (0 if unknown)
     * @param etag               optional current ETag
     * @return outcome (304/412/200). If 304/412 – status is already set and common validators written.
     */
    public static PreconditionResult evaluate(Headers requestHeaders, Headers responseHeaders, long lastModifiedMillis, ETag etag) {

        // Normalize LM to seconds granularity (HTTP-date precision)
        long lastModifiedSeconds = toHttpSecond(lastModifiedMillis);

        // 1) Always advertise validators when we know them
        if (etag != null) {
            responseHeaders.setHeader(HttpHeader.ETAG, etag.toHeaderValue());
        }

        if (lastModifiedSeconds > 0) {
            responseHeaders.setHeader(HttpHeader.LAST_MODIFIED, toHttpDate(lastModifiedSeconds));
        }

        // 2) If-Match / If-Unmodified-Since (preconditions for unsafe methods) — optional advanced path
        PreconditionResult strict = evaluateIfMatchAndIfUnmodifiedSince(requestHeaders, etag, lastModifiedSeconds);
        if (strict != PreconditionResult.PROCEED_200) {
            // 412 already chosen
            responseHeaders.setStatus(HttpStatus.PRECONDITION_FAILED);
            return PreconditionResult.PRECONDITION_FAILED_412;
        }

        // 3) If-None-Match has priority over If-Modified-Since
        String ifNoneMatch = header(requestHeaders, HttpHeader.IF_NONE_MATCH);
        if (ifNoneMatch != null) {
            boolean isGetHead = isHttpMethod(requestHeaders.getMethod().name(), HttpMethod.GET, HttpMethod.HEAD);
            if (etagMatchesAny(ifNoneMatch, etag, isGetHead)) {
                if (isGetHead) {
                    // 304 Not Modified for safe methods
                    setNotModified(responseHeaders);
                    return PreconditionResult.NOT_MODIFIED_304;
                }
                // 412 for methods that might change state (e.g., PUT, POST)
                responseHeaders.setStatus(HttpStatus.PRECONDITION_FAILED);
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
            // if none-match didn't match → proceed (ignore IMS)
            return PreconditionResult.PROCEED_200;
        }

        // 4) If-Modified-Since (only when If-None-Match is absent)
        String ifModifiedSince = header(requestHeaders, HttpHeader.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && lastModifiedSeconds > 0 && isHttpMethod(requestHeaders.getMethod().name(), HttpMethod.GET, HttpMethod.HEAD)) {

            long ifModifiedSinceSeconds = toHttpSeconds(ifModifiedSince);

            if (ifModifiedSinceSeconds >= 0 && lastModifiedSeconds <= ifModifiedSinceSeconds) {
                // not modified since IMS → 304
                setNotModified(responseHeaders);
                return PreconditionResult.NOT_MODIFIED_304;
            }
        }

        // 5) No preconditions preventing body → 200
        return PreconditionResult.PROCEED_200;
    }

    // ----------------------------- Internals ---------------------------------

    /**
     * If-Match / If-Unmodified-Since handling → 412 if fails.
     */
    private static PreconditionResult evaluateIfMatchAndIfUnmodifiedSince(Headers requestHeaders, ETag etag, long lastModifiedSeconds) {
        // If-Match: the current representation MUST match one of the listed tags
        String ifMatch = header(requestHeaders, HttpHeader.IF_MATCH);

        if (ifMatch != null) {
            if (etag == null) {
                return PreconditionResult.PRECONDITION_FAILED_412; // no tag to satisfy precondition
            }

            if (ifMatch.trim().equals("*")) {
                // exists → proceed; assume exists since we are evaluating
            } else if (!etagMatchesAny(ifMatch, etag, false)) {
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
        }

        // If-Unmodified-Since: fail if resource has been modified after given time
        String ifUnmodifiedSince = header(requestHeaders, HttpHeader.IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSince != null && lastModifiedSeconds > 0) {
            long iusSeconds = toHttpSeconds(ifUnmodifiedSince);
            if (iusSeconds >= 0 && lastModifiedSeconds > iusSeconds) {
                return PreconditionResult.PRECONDITION_FAILED_412;
            }
        }

        return PreconditionResult.PROCEED_200;
    }

    /**
     * Apply 304 response housekeeping.
     */
    private static void setNotModified(Headers responseHeaders) {
        responseHeaders.setStatus(HttpStatus.NOT_MODIFIED);
    }

    /**
     * Parse a plural If-None-Match value and compare against current ETag.
     */
    private static boolean etagMatchesAny(String header, ETag eTag, boolean allowWeak) {
        if (header == null || eTag == null) {
            return false;
        }

        String value = header.trim();

        if ("*".equals(value)) {
            return true;
        }

        List<ETag> eTags = ETag.parseList(value);

        for (ETag candidate : eTags) {
            return allowWeak ? eTag.weakEquals(candidate) : eTag.strongEquals(candidate);
        }

        // Split list: ETag entries are comma-separated; trim each
        String[] parts = value.split(",");

        for (String part : parts) {
            String token = part.trim();

            if (token.isEmpty()) {
                continue;
            }

            boolean candidateWeak = token.startsWith("W/");
            String  candidateCore = candidateWeak ? token.substring(2).trim() : token;

            // must be quoted-string (e.g., "abc")
            if (!candidateCore.startsWith("\"") || !candidateCore.endsWith("\"")) {
                continue;
            }

            ETag candidate = candidateWeak ? ETag.weak(candidateCore) : ETag.strong(candidateCore);

            // For GET/HEAD, weak comparison is allowed; for others, strong is required.
            return allowWeak ? eTag.weakEquals(candidate) : eTag.strongEquals(candidate);
        }

        return false;
    }

    private static String header(Headers requestHeaders, HttpHeader header) {
        return String.valueOf(requestHeaders.getHeader(header));
    }

    /**
     * Truncate milliseconds to seconds since epoch, per HTTP-date precision.
     */
    private static long toHttpSecond(long millis) {
        if (millis <= 0) {
            return 0;
        }

        return millis / 1000L;
    }

    /**
     * Format seconds to IMF-fixdate.
     */
    private static String toHttpDate(long epochSeconds) {
        return HTTP_DATE.format(Instant.ofEpochSecond(epochSeconds));
    }

    /**
     * Parse HTTP-date into seconds; returns -1 if parsing fails.
     */
    private static Instant toInstant(String raw) {
        try {
            return Instant.from(HTTP_DATE.parse(raw.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Parse HTTP-date into seconds; returns -1 if parsing fails.
     */
    private static long toHttpSeconds(String raw) {
        Instant instant = toInstant(raw);

        if (instant == null) {
            return -1;
        }

        return instant.getEpochSecond();
    }

    private static boolean isHttpMethod(String method, HttpMethod... methods) {
        return Set.of(methods).contains(HttpMethod.ofName(method));
    }

}
