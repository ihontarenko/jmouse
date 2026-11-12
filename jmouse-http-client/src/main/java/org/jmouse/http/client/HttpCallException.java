package org.jmouse.http.client;

/**
 * A call that got no answer: why, as one of the ways a call fails. It never carries the call's headers or body — a
 * credential must not reach a log through a failure.
 */
public final class HttpCallException extends RuntimeException {

    /** Why a call got no answer. */
    public enum Failure {
        /** It took longer than its time, the answer's body included. */
        TIMEOUT,
        /** Its answer's body was longer than the call may read. */
        RESPONSE_LIMIT,
        /** The connection failed: refused, reset, a name that does not resolve. */
        TRANSPORT,
        /** The calling thread was interrupted while it waited. */
        INTERRUPTED
    }

    private final Failure failure;

    public HttpCallException(Failure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public Failure failure() {
        return failure;
    }
}
