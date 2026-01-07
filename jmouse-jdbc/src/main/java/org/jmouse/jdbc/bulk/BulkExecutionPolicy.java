package org.jmouse.jdbc.bulk;

import org.jmouse.core.Verify;

public record BulkExecutionPolicy(
        int chunkSize,
        int parallelism,
        int maxInFlight,
        boolean failFast
) {
    public BulkExecutionPolicy {
        Verify.state(chunkSize <= 0, "chunkSize must be > 0");
        Verify.state(parallelism <= 0, "parallelism must be > 0");
        Verify.state(maxInFlight <= 0, "maxInFlight must be > 0");
    }

    public static BulkExecutionPolicy defaults() {
        return new BulkExecutionPolicy(1_000, 4, 8, true);
    }
}
