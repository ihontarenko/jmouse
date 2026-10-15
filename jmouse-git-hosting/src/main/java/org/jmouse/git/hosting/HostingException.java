package org.jmouse.git.hosting;

/**
 * A typed refusal from a hosting call. It never carries a token, a request header or anything the hosting sent in a
 * response body; only the reason, the HTTP status when there was one, and the rate-limit headers.
 */
public final class HostingException extends RuntimeException {

    /** Why a hosting call was refused. */
    public enum Reason {

        /** The address is not one this hosting serves, or names no repository. */
        ADDRESS_NOT_RECOGNISED,

        /** The hosting or the request is misconfigured: a bad base address, an invalid header value. */
        CONFIGURATION,

        /** The hosting rejected the credentials, or demands some (HTTP 401). */
        UNAUTHORISED,

        /** The credentials are accepted but may not do this (HTTP 403 without a rate-limit signal). */
        FORBIDDEN,

        /** No such repository — or one the credentials may not see; GitHub answers 404 for both. */
        NOT_FOUND,

        /** The hosting is throttling the caller; {@link HostingException#rateLimit()} says until when. Not retried here. */
        RATE_LIMITED,

        /** The request outlived its deadline. */
        TIMEOUT,

        /** The connection failed. */
        TRANSPORT,

        /** Any other non-success status, redirects included — they are never followed. */
        PROVIDER_REFUSED,

        /** The response was not the JSON shape this hosting documents. */
        INVALID_RESPONSE,

        /** The response body outgrew the configured ceiling. */
        RESPONSE_LIMIT
    }

    private final Reason    reason;
    private final Integer   status;
    private final RateLimit rateLimit;

    public HostingException(Reason reason) {
        this(reason, null, RateLimit.NOT_REPORTED);
    }

    public HostingException(Reason reason, Integer status, RateLimit rateLimit) {
        super("Git hosting request failed: " + reason + (status == null ? "" : " (HTTP " + status + ")"));
        this.reason = reason;
        this.status = status;
        this.rateLimit = rateLimit == null ? RateLimit.NOT_REPORTED : rateLimit;
    }

    /** Why the call was refused. */
    public Reason reason() {
        return reason;
    }

    /** The HTTP status, when the hosting answered at all. */
    public Integer status() {
        return status;
    }

    /** The rate-limit headers of the refusing response; {@link RateLimit#NOT_REPORTED} when there were none. */
    public RateLimit rateLimit() {
        return rateLimit;
    }

    /**
     * Whether repeating the same call may succeed without the caller changing anything. A rate-limited call is not
     * retryable in this sense: the caller waits until {@link RateLimit#resetsAt()} or {@link RateLimit#retryAfter()}.
     */
    public boolean retryable() {
        return reason == Reason.TIMEOUT || reason == Reason.TRANSPORT
                || (reason == Reason.PROVIDER_REFUSED && status != null && status >= 500);
    }
}
