package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.parser.JavaParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Owns disk paths and the persisted format's revision key; storage algorithms do not invent names.
 */
record IndexLayout(Path directory, Path stubs, String revisionKey) {

    private static final String PROJECTS_DIRECTORY = "projects";
    private static final String STUBS_DIRECTORY = "stubs";
    private static final String STUB_SUFFIX = ".stub";
    private static final String MANIFEST_FILENAME = "manifest";
    private static final String LOCK_FILENAME = "writer.lock";

    static IndexLayout create(Path cacheRoot, String scope, JavaParser.Level level) {
        String revisionKey =
                String.format(
                        "parser=%s;index=%s;format=%d;level=%s",
                        JavaParser.REVISION, FileIndex.REVISION, IndexBinaryCodec.VERSION, level);
        Path versionRoot = cacheRoot.toAbsolutePath().normalize().resolve(hash(revisionKey));
        Path project = versionRoot.resolve(PROJECTS_DIRECTORY).resolve(hash(scope));

        return new IndexLayout(project, versionRoot.resolve(STUBS_DIRECTORY), revisionKey);
    }

    Path manifest() {
        return directory.resolve(MANIFEST_FILENAME);
    }

    Path writerLock() {
        return directory.resolve(LOCK_FILENAME);
    }

    Path stub(String hash) {
        return stubs.resolve(new ContentHash(hash).value() + STUB_SUFFIX);
    }

    Path segment(String filename) {
        SegmentName segment =
                SegmentName.parse(filename)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid index segment name: " + filename));

        return directory.resolve(segment.filename());
    }

    private static String hash(String value) {
        return IndexFiles.hash(value.getBytes(StandardCharsets.UTF_8));
    }
}
