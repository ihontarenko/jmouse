package org.jmouse.jdbc.database;

public record DatabaseCapabilities(
        boolean supportsBatch,
        boolean supportsGeneratedKeys,
        boolean supportsSequences,
        boolean supportsOffsetFetch,
        boolean supportsReturning,
        boolean supportsScrollableResults
) {
    public static DatabaseCapabilities defaults() {
        return new DatabaseCapabilities(true, true, false, false, false, false);
    }
}
