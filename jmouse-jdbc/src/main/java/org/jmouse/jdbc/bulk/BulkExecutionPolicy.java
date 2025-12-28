package org.jmouse.jdbc.bulk;

import org.jmouse.core.Contract;

public record BulkExecutionPolicy(
        int chunkSize,
        int parallelism,
        int maxInFlight,
        boolean failFast
) {
    public BulkExecutionPolicy {
        Contract.state(chunkSize <= 0, "chunkSize must be > 0");
        Contract.state(parallelism <= 0, "parallelism must be > 0");
        Contract.state(maxInFlight <= 0, "maxInFlight must be > 0");
    }

    public static BulkExecutionPolicy defaults() {
        return new BulkExecutionPolicy(1_000, 4, 8, true);
    }
}
