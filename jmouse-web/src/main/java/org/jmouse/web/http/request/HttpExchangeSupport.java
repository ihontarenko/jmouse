package org.jmouse.web.http.request;

/**
 * 🧩 Thin adapter that mirrors Spring's ServletWebRequest "conditional" behavior
 * but works with jMouse Headers/HeadersBuffer.
 * <p>
 * Responsibilities:
 * - checkNotModified(etag/lastModified) with correct RFC precedence
 * - expose notModified flag (like Spring's isNotModified())
 * - write validators (ETag/Last-Modified) into response headers buffer
 */
public final class HttpExchangeSupport {

    private final Headers requestHeaders;
    private final Headers responseHeaders;

    private boolean notModified;

    public HttpExchangeSupport(Headers requestHeaders, Headers responseHeaders) {
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    /**
     * Equivalent to Spring's checkNotModified(lastModified).
     */
    public boolean checkNotModified(long lastModifiedMillis) {
        return checkNotModified(null, lastModifiedMillis);
    }

    /**
     * Equivalent to Spring's checkNotModified(etag).
     */
    public boolean checkNotModified(ETag etag) {
        return checkNotModified(etag, -1);
    }

    /**
     * Evaluate preconditions in RFC order, set status (304/412) and validators.
     * Returns true if request is "not modified" (i.e., body should not be sent).
     */
    public boolean checkNotModified(ETag etag, long lastModifiedMillis) {
        if (notModified) {
            return true;
        }

        PreconditionResult result = ConditionalRequest.evaluate(
                requestHeaders, responseHeaders, lastModifiedMillis, etag);

        if (result == PreconditionResult.NOT_MODIFIED_304) {
            this.notModified = true;
            // For GET/HEAD Spring also re-writes validators; we already wrote them in evaluate()
            return true;
        }

        if (result == PreconditionResult.PRECONDITION_FAILED_412) {
            this.notModified = true;
            // body not allowed
            return true;
        }

        // PROCEED_200: for safe methods advertise validators if known (done in evaluate()).
        return false;
    }

    public boolean isNotModified() {
        return notModified;
    }
}

