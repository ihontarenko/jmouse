package org.jmouse.git.hosting;

/**
 * Explicit per-request ceilings supplied by the adopter's policy.
 *
 * @param timeoutMilliseconds  the whole request's deadline, from sending to the last byte read
 * @param maximumResponseBytes the largest response body read; counted while streaming, before any JSON is parsed
 */
public record HostingLimits(int timeoutMilliseconds, int maximumResponseBytes) {

    /** Ten seconds and 8 MiB: a full page of a hundred repositories is well under a megabyte on either hosting. */
    public static final HostingLimits DEFAULT = new HostingLimits(10_000, 8 * 1024 * 1024);

    public HostingLimits {
        if (timeoutMilliseconds < 10 || timeoutMilliseconds > 120_000
                || maximumResponseBytes < 1024 || maximumResponseBytes > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("Explicit bounded hosting request limits are required.");
        }
    }
}
