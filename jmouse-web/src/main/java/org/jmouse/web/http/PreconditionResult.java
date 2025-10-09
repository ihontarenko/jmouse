package org.jmouse.web.http;

/**
 * 🔎 Outcome of conditional evaluation.
 */
public enum PreconditionResult {
    /**
     * Preconditions prevent returning a representation: respond 304 (GET/HEAD)
     */
    NOT_MODIFIED_304,
    /**
     * Preconditions fail for state-changing methods: respond 412
     */
    PRECONDITION_FAILED_412,
    /**
     * Proceed with normal 200 response
     */
    PROCEED_200
}