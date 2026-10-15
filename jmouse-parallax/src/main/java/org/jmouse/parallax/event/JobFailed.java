package org.jmouse.parallax.event;

/**
 * A queued computation failed for a reason that is not the request's: the engine itself broke.
 *
 * @param failureType the simple name of what was thrown
 */
public record JobFailed(String jobId, String failureType) {
}
