package org.jmouse.git.local;

/** Memory/response ceilings for an interactive local repository, independent of transfer limits. */
public record LocalGitLimits(
        int maximumFileBytes,
        int maximumLogEntries,
        int maximumDiffLines,
        int maximumSelectedFiles) {
    public static final LocalGitLimits DEFAULT =
            new LocalGitLimits(4 * 1024 * 1024, 200, 100_000, 1000);

    public LocalGitLimits {
        if (maximumFileBytes < 1
                || maximumLogEntries < 1
                || maximumDiffLines < 1
                || maximumSelectedFiles < 1) {
            throw new IllegalArgumentException("Local Git limits must be positive.");
        }
    }
}
