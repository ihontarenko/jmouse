package org.jmouse.jdbc.platform;

/**
 * Feature gates derived from platform and/or driver capabilities.
 *
 * <p>This is intentionally conservative at Stage 7. Feature-specific logic belongs to Stage 8.*.</p>
 */
public record PlatformCapabilities(
        boolean supportsBatch,
        boolean supportsGeneratedKeys,
        boolean supportsSequences,
        boolean supportsScrollableResults
) {
    public static PlatformCapabilities defaults() {
        return new PlatformCapabilities(true, true, false, false);
    }
}
