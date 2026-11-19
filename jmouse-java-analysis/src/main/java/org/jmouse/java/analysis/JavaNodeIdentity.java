package org.jmouse.java.analysis;

import java.util.Objects;

public record JavaNodeIdentity(String sourceName, String snapshot, int ordinal) {
    public JavaNodeIdentity {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(snapshot, "snapshot");
        if (ordinal < 0) {
            throw new IllegalArgumentException("Negative node ordinal");
        }
    }
}
