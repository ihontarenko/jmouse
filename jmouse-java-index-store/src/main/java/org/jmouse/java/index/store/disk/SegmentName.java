package org.jmouse.java.index.store.disk;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** The single naming rule for generated segments, shared by creation, restore and reclamation. */
record SegmentName(UUID identifier) {

    private static final String PREFIX = "index-";
    private static final String SUFFIX = ".segment";

    SegmentName {
        Objects.requireNonNull(identifier, "Segment identifier is required.");
    }

    static SegmentName create() {
        return new SegmentName(UUID.randomUUID());
    }

    static Optional<SegmentName> parse(String filename) {
        if (!filename.startsWith(PREFIX) || !filename.endsWith(SUFFIX)) {
            return Optional.empty();
        }

        String identifier =
                filename.substring(PREFIX.length(), filename.length() - SUFFIX.length());

        try {
            SegmentName segment = new SegmentName(UUID.fromString(identifier));
            return segment.filename().equals(filename) ? Optional.of(segment) : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    String filename() {
        return PREFIX + identifier + SUFFIX;
    }
}
