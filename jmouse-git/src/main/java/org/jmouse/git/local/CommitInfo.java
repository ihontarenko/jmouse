package org.jmouse.git.local;

import java.time.Instant;
import java.util.List;

public record CommitInfo(
        String id,
        List<String> parents,
        String author,
        Instant time,
        String subject,
        List<Reference> labels) {
    public CommitInfo {
        parents = List.copyOf(parents);
        labels = List.copyOf(labels);
    }

    public record Reference(String name, Kind kind) {}

    public enum Kind {
        BRANCH,
        REMOTE,
        TAG
    }
}
