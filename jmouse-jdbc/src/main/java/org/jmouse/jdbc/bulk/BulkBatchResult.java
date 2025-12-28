package org.jmouse.jdbc.bulk;

import java.util.List;

public record BulkBatchResult(
        int chunks,
        long affectedRows,
        List<Throwable> errors
) {

    public static BulkBatchResult empty() {
        return new BulkBatchResult(0, 0, List.of());
    }

    public boolean isFailed() {
        return !isSuccess();
    }

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

}
