package org.jmouse.git;

import java.time.Duration;

/**
 * Explicit per-call ceilings supplied by the adopter's policy.
 *
 * @param timeout      the whole operation's deadline; it also bounds how long the remote may stay silent
 * @param maximumBytes the most the target directory may hold on disk, repository metadata included — checked while
 *                     objects arrive and again once the checkout is complete
 * @param depth        how many commits of history to fetch; {@code 1} is a shallow clone of the tip alone
 */
public record GitLimits(Duration timeout, long maximumBytes, int depth) {

    // Declared before DEFAULT: static fields initialise in textual order, and DEFAULT's constructor reads these.
    private static final Duration MINIMUM_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration MAXIMUM_TIMEOUT = Duration.ofHours(1);
    private static final long     MINIMUM_BYTES   = 1024;
    private static final long     MAXIMUM_BYTES   = 1024L * 1024 * 1024 * 1024;
    private static final int      MAXIMUM_DEPTH   = 100_000;

    /** 120 seconds, 512 MiB, depth 1 — the ceilings Raccoon's snapshots ran under. */
    public static final GitLimits DEFAULT = new GitLimits(Duration.ofSeconds(120), 512L * 1024 * 1024, 1);

    public GitLimits {
        if (timeout == null || timeout.compareTo(MINIMUM_TIMEOUT) < 0 || timeout.compareTo(MAXIMUM_TIMEOUT) > 0) {
            throw new IllegalArgumentException("A Git timeout between one second and one hour is required.");
        }
        if (maximumBytes < MINIMUM_BYTES || maximumBytes > MAXIMUM_BYTES) {
            throw new IllegalArgumentException("A Git size ceiling between 1 KiB and 1 TiB is required.");
        }
        if (depth < 1 || depth > MAXIMUM_DEPTH) {
            throw new IllegalArgumentException("A Git history depth between 1 and " + MAXIMUM_DEPTH + " is required.");
        }
    }

    /** These limits with another deadline. */
    public GitLimits withTimeout(Duration timeout) {
        return new GitLimits(timeout, maximumBytes, depth);
    }

    /** These limits with another size ceiling. */
    public GitLimits withMaximumBytes(long maximumBytes) {
        return new GitLimits(timeout, maximumBytes, depth);
    }

    /** These limits with another history depth. */
    public GitLimits withDepth(int depth) {
        return new GitLimits(timeout, maximumBytes, depth);
    }

    /** The deadline in whole seconds, the unit JGit's network timeout takes; never less than one. */
    int timeoutSeconds() {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, timeout.toSeconds()));
    }
}
