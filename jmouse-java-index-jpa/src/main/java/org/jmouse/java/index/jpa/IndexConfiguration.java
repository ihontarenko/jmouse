package org.jmouse.java.index.jpa;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.store.disk.IndexBinaryCodec;
import org.jmouse.java.parser.JavaParser;

import java.util.Objects;

/** Every algorithm/format choice that affects reusable contributions. */
public record IndexConfiguration(
        String parserRevision,
        String indexRevision,
        String payloadRevision,
        JavaParser.Level level) {
    public IndexConfiguration {
        Objects.requireNonNull(level);
        for (String revision : new String[] {parserRevision, indexRevision, payloadRevision}) {
            if (revision == null || !revision.matches("[A-Za-z0-9.:-]{1,48}")) {
                throw new IllegalArgumentException(
                        "Index revisions must be explicit bounded identifiers.");
            }
        }
    }

    public static IndexConfiguration current(JavaParser.Level level) {
        return new IndexConfiguration(
                JavaParser.REVISION, FileIndex.REVISION, IndexBinaryCodec.revision(), level);
    }

    public String key() {
        return "parser="
                + parserRevision
                + ";index="
                + indexRevision
                + ";payload="
                + payloadRevision
                + ";level="
                + level;
    }
}
